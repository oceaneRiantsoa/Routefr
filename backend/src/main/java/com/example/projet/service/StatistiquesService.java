package com.example.projet.service;

import com.example.projet.dto.AvancementDTO;
import com.example.projet.dto.StatistiquesDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.entity.SignalementStatus;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.repository.SignalementStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour calculer les statistiques et gérer l'avancement des signalements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatistiquesService {

    private final SignalementFirebaseRepository firebaseRepository;
    private final SignalementStatusRepository statusRepository;

    /**
     * Calcule les statistiques complètes avec délais de traitement
     */
    public StatistiquesDTO getStatistiquesCompletes() {
        List<SignalementFirebase> signalements = firebaseRepository.findAll();
        
        if (signalements.isEmpty()) {
            return StatistiquesDTO.builder()
                    .nombreTotal(0L)
                    .nombreNouveau(0L)
                    .nombreEnCours(0L)
                    .nombreTermine(0L)
                    .nombreRejete(0L)
                    .delaiMoyenTraitement(0.0)
                    .delaiMoyenDebutTravaux(0.0)
                    .delaiMoyenFinTravaux(0.0)
                    .delaiParType(new HashMap<>())
                    .pourcentageNouveau(0.0)
                    .pourcentageEnCours(0.0)
                    .pourcentageTermine(0.0)
                    .pourcentageRejete(0.0)
                    .build();
        }

        long total = signalements.size();
        
        // Compter par statut/avancement
        long nouveau = signalements.stream()
                .filter(s -> getAvancement(s) == 0)
                .count();
        long enCours = signalements.stream()
                .filter(s -> getAvancement(s) == 50)
                .count();
        long termine = signalements.stream()
                .filter(s -> getAvancement(s) == 100)
                .count();
        long rejete = signalements.stream()
                .filter(s -> "rejete".equalsIgnoreCase(s.getStatus()) || "rejeté".equalsIgnoreCase(s.getStatus()))
                .count();

        // Calculer les délais moyens
        double delaiMoyenTraitement = calculerDelaiMoyenTraitement(signalements);
        double delaiMoyenDebutTravaux = calculerDelaiMoyenDebutTravaux(signalements);
        double delaiMoyenFinTravaux = calculerDelaiMoyenFinTravaux(signalements);
        
        // Délais par type de problème
        Map<String, Double> delaiParType = calculerDelaisParType(signalements);

        // Pourcentages
        double pNouveau = total > 0 ? (nouveau * 100.0 / total) : 0;
        double pEnCours = total > 0 ? (enCours * 100.0 / total) : 0;
        double pTermine = total > 0 ? (termine * 100.0 / total) : 0;
        double pRejete = total > 0 ? (rejete * 100.0 / total) : 0;

        return StatistiquesDTO.builder()
                .nombreTotal(total)
                .nombreNouveau(nouveau)
                .nombreEnCours(enCours)
                .nombreTermine(termine)
                .nombreRejete(rejete)
                .delaiMoyenTraitement(Math.round(delaiMoyenTraitement * 10.0) / 10.0)
                .delaiMoyenDebutTravaux(Math.round(delaiMoyenDebutTravaux * 10.0) / 10.0)
                .delaiMoyenFinTravaux(Math.round(delaiMoyenFinTravaux * 10.0) / 10.0)
                .delaiParType(delaiParType)
                .pourcentageNouveau(Math.round(pNouveau * 10.0) / 10.0)
                .pourcentageEnCours(Math.round(pEnCours * 10.0) / 10.0)
                .pourcentageTermine(Math.round(pTermine * 10.0) / 10.0)
                .pourcentageRejete(Math.round(pRejete * 10.0) / 10.0)
                .build();
    }

    /**
     * Détermine l'avancement d'un signalement
     */
    private int getAvancement(SignalementFirebase s) {
        if (s.getAvancementPourcentage() != null) {
            return s.getAvancementPourcentage();
        }
        // Déduire de l'ancien statut si pas encore migré
        String status = s.getStatus() != null ? s.getStatus().toLowerCase() : "";
        String statutLocal = s.getStatutLocal() != null ? s.getStatutLocal().toLowerCase() : "";
        
        if (status.contains("termin") || statutLocal.contains("termin") || status.equals("traite")) {
            return 100;
        } else if (status.contains("cours") || statutLocal.contains("cours")) {
            return 50;
        }
        return 0;
    }

    /**
     * Calcule le délai moyen de traitement complet (création -> fin)
     */
    private double calculerDelaiMoyenTraitement(List<SignalementFirebase> signalements) {
        List<Long> delais = signalements.stream()
                .filter(s -> s.getDateFinTravaux() != null && s.getDateCreationFirebase() != null)
                .map(s -> ChronoUnit.DAYS.between(s.getDateCreationFirebase(), s.getDateFinTravaux()))
                .collect(Collectors.toList());
        
        if (delais.isEmpty()) return 0.0;
        return delais.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Calcule le délai moyen pour le début des travaux (création -> début)
     */
    private double calculerDelaiMoyenDebutTravaux(List<SignalementFirebase> signalements) {
        List<Long> delais = signalements.stream()
                .filter(s -> s.getDateDebutTravaux() != null && s.getDateCreationFirebase() != null)
                .map(s -> ChronoUnit.DAYS.between(s.getDateCreationFirebase(), s.getDateDebutTravaux()))
                .collect(Collectors.toList());
        
        if (delais.isEmpty()) return 0.0;
        return delais.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Calcule le délai moyen des travaux (début -> fin)
     */
    private double calculerDelaiMoyenFinTravaux(List<SignalementFirebase> signalements) {
        List<Long> delais = signalements.stream()
                .filter(s -> s.getDateFinTravaux() != null && s.getDateDebutTravaux() != null)
                .map(s -> ChronoUnit.DAYS.between(s.getDateDebutTravaux(), s.getDateFinTravaux()))
                .collect(Collectors.toList());
        
        if (delais.isEmpty()) return 0.0;
        return delais.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /**
     * Calcule les délais moyens par type de problème
     */
    private Map<String, Double> calculerDelaisParType(List<SignalementFirebase> signalements) {
        Map<String, List<Long>> delaisParType = new HashMap<>();
        
        for (SignalementFirebase s : signalements) {
            if (s.getDateFinTravaux() != null && s.getDateCreationFirebase() != null && s.getProblemeNom() != null) {
                String type = s.getProblemeNom();
                long delai = ChronoUnit.DAYS.between(s.getDateCreationFirebase(), s.getDateFinTravaux());
                delaisParType.computeIfAbsent(type, k -> new ArrayList<>()).add(delai);
            }
        }
        
        Map<String, Double> moyennes = new LinkedHashMap<>();
        delaisParType.forEach((type, delais) -> {
            double moyenne = delais.stream().mapToLong(Long::longValue).average().orElse(0.0);
            moyennes.put(type, Math.round(moyenne * 10.0) / 10.0);
        });
        
        // Trier par délai décroissant
        return moyennes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Met à jour l'avancement d'un signalement
     */
    @Transactional
    public SignalementFirebase updateAvancement(Long id, AvancementDTO avancementDTO) {
        SignalementFirebase signalement = firebaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé: " + id));

        LocalDateTime now = LocalDateTime.now();
        String statut = avancementDTO.getStatut().toLowerCase();
        int pourcentage = avancementDTO.getPourcentage() != null ? avancementDTO.getPourcentage() : 0;
        int codeStatut;

        // Déterminer le pourcentage selon le statut ET le code pour signalement_status
        if (statut.contains("nouveau") || statut.equals("non_traite") || pourcentage == 0) {
            pourcentage = 0;
            codeStatut = 10; // EN_ATTENTE
            signalement.setStatus("nouveau");
            // Réinitialiser les dates si retour à nouveau
            signalement.setDateDebutTravaux(null);
            signalement.setDateFinTravaux(null);
        } else if (statut.contains("cours") || pourcentage == 50) {
            pourcentage = 50;
            codeStatut = 20; // EN_COURS
            signalement.setStatus("en_cours");
            // Enregistrer la date de début si pas déjà définie
            if (signalement.getDateDebutTravaux() == null) {
                signalement.setDateDebutTravaux(now);
            }
            // Effacer la date de fin si on revient en cours
            signalement.setDateFinTravaux(null);
        } else if (statut.contains("termin") || statut.equals("traite") || pourcentage == 100) {
            pourcentage = 100;
            codeStatut = 30; // TERMINE/TRAITE
            signalement.setStatus("termine");
            // Enregistrer la date de fin
            signalement.setDateFinTravaux(now);
            // Si pas de date de début, la définir aussi
            if (signalement.getDateDebutTravaux() == null) {
                signalement.setDateDebutTravaux(now);
            }
        } else if (statut.contains("rejet") || statut.equals("rejete")) {
            pourcentage = 0;
            codeStatut = 40; // REJETE
            signalement.setStatus("rejete");
            // Réinitialiser les dates si rejeté
            signalement.setDateDebutTravaux(null);
            signalement.setDateFinTravaux(null);
        } else {
            // Par défaut
            codeStatut = mapPourcentageToStatut(pourcentage);
        }

        signalement.setAvancementPourcentage(pourcentage);
        signalement.setDateModificationLocal(now);
        
        // Synchroniser avec la table signalement_status
        updateSignalementStatus(id, codeStatut);
        
        // Ajouter les notes si fournies
        if (avancementDTO.getNotes() != null && !avancementDTO.getNotes().isEmpty()) {
            String existingNotes = signalement.getNotesManager() != null ? signalement.getNotesManager() : "";
            String newNote = String.format("[%s] %s: %s", now.toLocalDate(), statut.toUpperCase(), avancementDTO.getNotes());
            signalement.setNotesManager(existingNotes.isEmpty() ? newNote : existingNotes + "\n" + newNote);
        }

        log.info("Mise à jour avancement signalement {}: {}% ({}) - Code statut: {}", id, pourcentage, statut, codeStatut);
        return firebaseRepository.save(signalement);
    }

    /**
     * Map pourcentage vers code de statut
     */
    private Integer mapPourcentageToStatut(int pourcentage) {
        if (pourcentage == 0) return 10;        // EN_ATTENTE
        else if (pourcentage == 50) return 20;  // EN_COURS
        else if (pourcentage == 100) return 30; // TERMINE/TRAITE
        else return 10; // Par défaut EN_ATTENTE
    }

    /**
     * Met à jour la table signalement_status avec le nouveau code de statut
     */
    @Transactional
    private void updateSignalementStatus(Long signalementId, Integer codeStatut) {
        try {
            // Chercher le statut existant
            Optional<SignalementStatus> existingStatus = statusRepository.findByIdSignalement(signalementId);
            
            if (existingStatus.isPresent()) {
                // Mettre à jour le statut existant
                SignalementStatus status = existingStatus.get();
                status.setIdStatut(codeStatut);
                statusRepository.save(status);
                log.info("Statut mis à jour pour signalement {}: code {}", signalementId, codeStatut);
            } else {
                // Créer un nouveau statut
                SignalementStatus newStatus = SignalementStatus.builder()
                    .idSignalement(signalementId.intValue())
                    .idStatut(codeStatut)
                    .build();
                statusRepository.save(newStatus);
                log.info("Nouveau statut créé pour signalement {}: code {}", signalementId, codeStatut);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut pour signalement {}: {}", signalementId, e.getMessage());
            // Ne pas faire échouer la transaction principale
        }
    }

    /**
     * Récupère un signalement par ID
     */
    public Optional<SignalementFirebase> getSignalementById(Long id) {
        return firebaseRepository.findById(id);
    }
}
