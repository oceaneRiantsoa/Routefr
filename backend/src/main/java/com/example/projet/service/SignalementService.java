package com.example.projet.service;

import com.example.projet.dto.EntrepriseDTO;
import com.example.projet.dto.SignalementDTO;
import com.example.projet.dto.SignalementUpdateDTO;
import com.example.projet.entity.HistoriqueAvancement;
import com.example.projet.entity.SignalementDetails;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.entity.SignalementStatus;
import com.example.projet.repository.HistoriqueAvancementRepository;
import com.example.projet.repository.SignalementDetailsRepository;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.repository.SignalementStatusRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalementService {

    private final SignalementDetailsRepository repository;
    private final SignalementFirebaseRepository firebaseRepository;
    private final SignalementStatusRepository statusRepository;
    private final HistoriqueAvancementRepository historiqueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Offset pour distinguer les IDs Firebase des IDs locaux
    private static final long FIREBASE_ID_OFFSET = 10000L;

    /**
     * Récupère tous les signalements pour le manager (locaux + Firebase)
     */
    public List<SignalementDTO> getAllSignalements() {
        List<SignalementDTO> allSignalements = new ArrayList<>();
        
        // 1. Signalements locaux (signalement_details)
        List<Object[]> results = repository.findAllSignalementsForManager();
        List<SignalementDTO> localSignalements = results.stream()
                .map(this::mapToSignalementDTO)
                .collect(Collectors.toList());
        allSignalements.addAll(localSignalements);
        log.debug("📍 Signalements locaux: {}", localSignalements.size());
        
        // 2. Signalements Firebase (signalement_firebase)
        List<SignalementFirebase> firebaseSignalements = firebaseRepository.findAll();
        List<SignalementDTO> firebaseDTOs = firebaseSignalements.stream()
                .map(this::mapFirebaseToDTO)
                .collect(Collectors.toList());
        allSignalements.addAll(firebaseDTOs);
        log.debug("🔥 Signalements Firebase: {}", firebaseDTOs.size());
        
        log.info("📋 Total signalements (Manager): {}", allSignalements.size());
        return allSignalements;
    }

    /**
     * Récupère les signalements filtrés par statut
     */
    public List<SignalementDTO> getSignalementsByStatut(Integer idStatut) {
        List<Object[]> results = repository.findAllSignalementsForManagerByStatut(idStatut);
        return results.stream()
                .map(this::mapToSignalementDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un signalement par son ID
     */
    public Optional<SignalementDTO> getSignalementById(Long id) {
        // Récupérer tous et filtrer par ID (on pourrait optimiser avec une requête dédiée)
        return getAllSignalements().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    /**
     * Met à jour un signalement (infos manager + statut)
     * Gère les signalements locaux (ID < 10000) et Firebase (ID >= 10000)
     */
    @Transactional
    public SignalementDTO updateSignalement(Long id, SignalementUpdateDTO updateDTO) {
        // Déterminer si c'est un signalement Firebase (ID >= 10000)
        if (id >= FIREBASE_ID_OFFSET) {
            return updateSignalementFirebase(id - FIREBASE_ID_OFFSET, updateDTO);
        } else {
            return updateSignalementLocal(id, updateDTO);
        }
    }
    
    /**
     * Met à jour un signalement local (signalement_details)
     */
    @Transactional
    protected SignalementDTO updateSignalementLocal(Long id, SignalementUpdateDTO updateDTO) {
        SignalementDetails entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement local non trouvé avec l'ID: " + id));

        // Sauvegarder l'ancien statut pour l'historique
        Integer ancienStatut = null;
        if (entity.getIdSignalement() != null) {
            Optional<SignalementStatus> existingStatus = statusRepository.findByIdSignalement(entity.getIdSignalement().longValue());
            if (existingStatus.isPresent()) {
                ancienStatut = existingStatus.get().getIdStatut();
            }
        }

        // Mise à jour des champs de signalement_details
        if (updateDTO.getSurface() != null) {
            entity.setSurface(updateDTO.getSurface());
        }
        if (updateDTO.getBudgetEstime() != null) {
            entity.setBudgetEstime(updateDTO.getBudgetEstime());
        }
        if (updateDTO.getIdEntreprise() != null) {
            entity.setIdEntreprise(updateDTO.getIdEntreprise());
        }
        if (updateDTO.getNotesManager() != null) {
            entity.setNotesManager(updateDTO.getNotesManager());
        }
        
        // Date de modification
        entity.setDateModification(LocalDateTime.now());

        repository.save(entity);

        // Mise à jour du statut dans signalement_status
        if (updateDTO.getIdStatut() != null && entity.getIdSignalement() != null) {
            Optional<SignalementStatus> existingStatus = statusRepository.findByIdSignalement(entity.getIdSignalement().longValue());
            
            if (existingStatus.isPresent()) {
                SignalementStatus status = existingStatus.get();
                status.setIdStatut(updateDTO.getIdStatut());
                statusRepository.save(status);
            } else {
                // Créer un nouveau statut si n'existe pas
                SignalementStatus newStatus = SignalementStatus.builder()
                        .idSignalement(entity.getIdSignalement().intValue())
                        .idStatut(updateDTO.getIdStatut())
                        .build();
                statusRepository.save(newStatus);
            }
            
            // Enregistrer dans l'historique si le statut a changé
            if (ancienStatut == null || !ancienStatut.equals(updateDTO.getIdStatut())) {
                Integer ancienAvancement = mapStatutToAvancement(ancienStatut);
                Integer nouveauAvancement = mapStatutToAvancement(updateDTO.getIdStatut());
                
                HistoriqueAvancement historique = HistoriqueAvancement.builder()
                        .signalementId(id)
                        .ancienStatut(ancienStatut != null ? ancienStatut.toString() : null)
                        .nouveauStatut(updateDTO.getIdStatut().toString())
                        .ancienAvancement(ancienAvancement)
                        .nouveauAvancement(nouveauAvancement)
                        .dateChangement(LocalDateTime.now())
                        .commentaire(updateDTO.getNotesManager())
                        .build();
                historiqueRepository.save(historique);
                log.info("📝 Historique enregistré pour signalement local {}: {} -> {}", 
                         id, ancienStatut, updateDTO.getIdStatut());
            }
            
            log.info("Statut {} mis à jour pour signalement local {}", updateDTO.getIdStatut(), id);
        }

        log.info("Signalement local {} mis à jour avec statut: {}", id, updateDTO.getIdStatut());

        // Retourner le signalement mis à jour
        return getSignalementById(id).orElseThrow();
    }
    
    /**
     * Met à jour un signalement Firebase (signalement_firebase)
     */
    @Transactional
    protected SignalementDTO updateSignalementFirebase(Long firebaseDbId, SignalementUpdateDTO updateDTO) {
        SignalementFirebase entity = firebaseRepository.findById(firebaseDbId)
                .orElseThrow(() -> new RuntimeException("Signalement Firebase non trouvé avec l'ID: " + firebaseDbId));

        // Sauvegarder les anciennes valeurs pour l'historique
        String ancienStatut = entity.getStatutLocal() != null ? entity.getStatutLocal() : entity.getStatus();
        Integer ancienAvancement = entity.getAvancementPourcentage();

        // Mise à jour des champs
        if (updateDTO.getSurface() != null) {
            entity.setSurface(updateDTO.getSurface());
        }
        if (updateDTO.getBudgetEstime() != null) {
            entity.setBudgetEstime(updateDTO.getBudgetEstime());
        }
        if (updateDTO.getNotesManager() != null) {
            entity.setNotesManager(updateDTO.getNotesManager());
        }
        
        // Mise à jour de l'entreprise
        if (updateDTO.getIdEntreprise() != null) {
            entity.setEntrepriseId(updateDTO.getIdEntreprise().toString());
            // Récupérer le nom de l'entreprise
            List<EntrepriseDTO> entreprises = getAllEntreprises();
            entreprises.stream()
                    .filter(e -> e.getId().equals(updateDTO.getIdEntreprise().longValue()))
                    .findFirst()
                    .ifPresent(e -> entity.setEntrepriseNom(e.getNomEntreprise()));
        }
        
        // Mise à jour du statut et de l'avancement
        String nouveauStatut = null;
        Integer nouveauAvancement = null;
        
        if (updateDTO.getIdStatut() != null) {
            // Convertir l'ID statut en code texte Firebase
            switch (updateDTO.getIdStatut()) {
                case 10:
                    nouveauStatut = "nouveau";
                    nouveauAvancement = 0;
                    break;
                case 20:
                    nouveauStatut = "en_cours";
                    nouveauAvancement = 50;
                    // Enregistrer la date de début de travaux si première fois
                    if (entity.getDateDebutTravaux() == null) {
                        entity.setDateDebutTravaux(LocalDateTime.now());
                    }
                    break;
                case 30:
                    nouveauStatut = "traite";
                    nouveauAvancement = 100;
                    // Enregistrer la date de fin de travaux
                    entity.setDateFinTravaux(LocalDateTime.now());
                    break;
                case 40:
                    nouveauStatut = "rejete";
                    nouveauAvancement = 0;
                    break;
            }
            
            entity.setStatutLocal(nouveauStatut);
            entity.setStatus(nouveauStatut); // Aussi mettre à jour le status principal
            entity.setAvancementPourcentage(nouveauAvancement);
        }
        
        // Date de modification
        entity.setDateModificationLocal(LocalDateTime.now());
        
        firebaseRepository.save(entity);
        
        // Enregistrer dans l'historique si le statut a changé
        if (nouveauStatut != null && !nouveauStatut.equals(ancienStatut)) {
            HistoriqueAvancement historique = HistoriqueAvancement.builder()
                    .firebaseSignalementId(firebaseDbId)
                    .ancienStatut(ancienStatut)
                    .nouveauStatut(nouveauStatut)
                    .ancienAvancement(ancienAvancement)
                    .nouveauAvancement(nouveauAvancement)
                    .dateChangement(LocalDateTime.now())
                    .commentaire(updateDTO.getNotesManager())
                    .build();
            historiqueRepository.save(historique);
            log.info("📝 Historique enregistré pour signalement Firebase {}: {} -> {}", 
                     firebaseDbId, ancienStatut, nouveauStatut);
        }

        log.info("Signalement Firebase {} mis à jour avec statut: {}", firebaseDbId, nouveauStatut);

        // Retourner le signalement mis à jour (avec l'offset)
        return getSignalementById(firebaseDbId + FIREBASE_ID_OFFSET).orElseThrow();
    }
    
    /**
     * Récupère l'historique d'avancement d'un signalement
     */
    public List<HistoriqueAvancement> getHistoriqueAvancement(Long id) {
        if (id >= FIREBASE_ID_OFFSET) {
            return historiqueRepository.findByFirebaseSignalementIdOrderByDateChangementAsc(id - FIREBASE_ID_OFFSET);
        } else {
            return historiqueRepository.findBySignalementIdOrderByDateChangementAsc(id);
        }
    }
    
    /**
     * Calcule les statistiques de temps de traitement moyen
     */
    public Map<String, Object> getStatistiquesTraitement() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Temps moyen de prise en charge (nouveau -> en_cours)
        Double tempsPriseEnChargeLocal = historiqueRepository.calculerTempsMoyenPriseEnChargeLocal();
        Double tempsPriseEnChargeFirebase = historiqueRepository.calculerTempsMoyenPriseEnChargeFirebase();
        
        // Temps moyen de traitement (en_cours -> traité)
        Double tempsTraitementLocal = historiqueRepository.calculerTempsMoyenTraitementLocal();
        Double tempsTraitementFirebase = historiqueRepository.calculerTempsMoyenTraitementFirebase();
        
        // Convertir en format lisible (jours, heures, minutes)
        stats.put("tempsPriseEnChargeMoyenSecondes", moyenneNullSafe(tempsPriseEnChargeLocal, tempsPriseEnChargeFirebase));
        stats.put("tempsPriseEnChargeMoyenFormate", formatDuration(moyenneNullSafe(tempsPriseEnChargeLocal, tempsPriseEnChargeFirebase)));
        
        stats.put("tempsTraitementMoyenSecondes", moyenneNullSafe(tempsTraitementLocal, tempsTraitementFirebase));
        stats.put("tempsTraitementMoyenFormate", formatDuration(moyenneNullSafe(tempsTraitementLocal, tempsTraitementFirebase)));
        
        // Total du cycle (nouveau -> traité)
        Double totalCycle = null;
        if (stats.get("tempsPriseEnChargeMoyenSecondes") != null && stats.get("tempsTraitementMoyenSecondes") != null) {
            totalCycle = (Double) stats.get("tempsPriseEnChargeMoyenSecondes") + (Double) stats.get("tempsTraitementMoyenSecondes");
        }
        stats.put("tempsCycleTotalSecondes", totalCycle);
        stats.put("tempsCycleTotalFormate", formatDuration(totalCycle));
        
        // Nombre de changements par statut
        List<Object[]> changementsParStatut = historiqueRepository.countByNouveauStatut();
        Map<String, Long> comptageStatuts = new LinkedHashMap<>();
        for (Object[] row : changementsParStatut) {
            comptageStatuts.put(row[0] != null ? row[0].toString() : "inconnu", ((Number) row[1]).longValue());
        }
        stats.put("changementsParStatut", comptageStatuts);
        
        return stats;
    }
    
    /**
     * Calcule la moyenne de deux valeurs en ignorant les nulls
     */
    private Double moyenneNullSafe(Double val1, Double val2) {
        if (val1 == null && val2 == null) return null;
        if (val1 == null) return val2;
        if (val2 == null) return val1;
        return (val1 + val2) / 2;
    }
    
    /**
     * Formate une durée en secondes en format lisible
     */
    private String formatDuration(Double secondes) {
        if (secondes == null) return "N/A";
        
        long totalSecondes = secondes.longValue();
        long jours = totalSecondes / 86400;
        long heures = (totalSecondes % 86400) / 3600;
        long minutes = (totalSecondes % 3600) / 60;
        
        if (jours > 0) {
            return String.format("%dj %dh %dmin", jours, heures, minutes);
        } else if (heures > 0) {
            return String.format("%dh %dmin", heures, minutes);
        } else {
            return String.format("%dmin", minutes);
        }
    }

    /**
     * Récupère les statistiques par statut (signalements locaux + Firebase)
     */
    public Map<String, Long> getStatistiquesByStatut() {
        Map<String, Long> stats = new LinkedHashMap<>();
        
        // Initialiser tous les statuts à 0
        stats.put("EN_ATTENTE", 0L);
        stats.put("EN_COURS", 0L);
        stats.put("TRAITE", 0L);
        stats.put("REJETE", 0L);
        
        // 1. Compter les signalements locaux (signalement_details)
        List<Object[]> localResults = repository.countByStatut();
        for (Object[] row : localResults) {
            Integer idStatut = row[0] != null ? ((Number) row[0]).intValue() : 10;
            Long count = ((Number) row[1]).longValue();
            String code = SignalementDTO.getStatutCode(idStatut);
            stats.put(code, stats.getOrDefault(code, 0L) + count);
        }
        
        // 2. Compter les signalements Firebase (signalement_firebase)
        List<Object[]> firebaseResults = firebaseRepository.countByStatusGrouped();
        for (Object[] row : firebaseResults) {
            String status = row[0] != null ? row[0].toString() : "nouveau";
            Long count = ((Number) row[1]).longValue();
            // Mapper le status Firebase vers le code statut
            String code = mapFirebaseStatusToCode(status);
            stats.put(code, stats.getOrDefault(code, 0L) + count);
        }
        
        return stats;
    }
    
    /**
     * Mapper le status Firebase vers le code statut local
     */
    private String mapFirebaseStatusToCode(String firebaseStatus) {
        if (firebaseStatus == null) return "EN_ATTENTE";
        switch (firebaseStatus.toLowerCase()) {
            case "en_cours":
            case "en cours":
                return "EN_COURS";
            case "traite":
            case "traité":
                return "TRAITE";
            case "rejete":
            case "rejeté":
                return "REJETE";
            case "nouveau":
            case "non_traite":
            default:
                return "EN_ATTENTE";
        }
    }

    /**
     * Récupère la liste des entreprises disponibles
     */
    public List<EntrepriseDTO> getAllEntreprises() {
        List<Object[]> results = repository.findAllEntreprises();
        return results.stream()
                .map(row -> EntrepriseDTO.builder()
                        .id(((Number) row[0]).longValue())
                        .nomEntreprise(row[1] != null ? row[1].toString() : null)
                        .localisation(row[2] != null ? row[2].toString() : null)
                        .contact(row[3] != null ? row[3].toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convertit un résultat de requête native en DTO
     */
    private SignalementDTO mapToSignalementDTO(Object[] row) {
        BigDecimal surface = row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO;
        BigDecimal coutParM2 = row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;
        BigDecimal budgetCalcule = surface.multiply(coutParM2);
        
        Integer idStatut = row[14] != null ? ((Number) row[14]).intValue() : 10;
        
        // Calculer l'avancement basé sur le statut pour les signalements locaux
        Integer avancementPourcentage = mapStatutToAvancement(idStatut);

        return SignalementDTO.builder()
                .id(((Number) row[0]).longValue())
                .idSignalement(row[1] != null ? ((Number) row[1]).intValue() : null)
                .latitude(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                .longitude(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                .probleme(row[4] != null ? row[4].toString() : null)
                .dateSignalement(row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null)
                .surface(surface)
                .coutParM2(coutParM2)
                .idEntreprise(row[8] != null ? ((Number) row[8]).intValue() : null)
                .entrepriseNom(row[9] != null ? row[9].toString() : null)
                .commentaires(row[10] != null ? row[10].toString() : null)
                .budgetEstime(row[11] != null ? new BigDecimal(row[11].toString()) : null)
                .notesManager(row[12] != null ? row[12].toString() : null)
                .dateModification(row[13] != null ? ((java.sql.Timestamp) row[13]).toLocalDateTime() : null)
                .idStatut(idStatut)
                .statutLibelle(SignalementDTO.getStatutLibelle(idStatut))
                .budgetCalcule(budgetCalcule)
                .avancementPourcentage(avancementPourcentage)
                .build();
    }

    /**
     * Convertit une entité SignalementFirebase en DTO
     */
    private SignalementDTO mapFirebaseToDTO(SignalementFirebase entity) {
        // Offset de 10000 pour éviter les conflits d'ID avec les signalements locaux
        Long dtoId = 10000L + entity.getId();
        
        // Mapper le statut Firebase vers les ID de statut locaux
        Integer idStatut;
        // Utiliser statutLocal sauf si c'est la valeur par défaut "non_traite" (pas encore traité par le manager)
        String status;
        if (entity.getStatutLocal() != null && !"non_traite".equals(entity.getStatutLocal())) {
            status = entity.getStatutLocal();
        } else {
            status = entity.getStatus() != null ? entity.getStatus() : "nouveau";
        }
        
        switch (status.toLowerCase()) {
            case "en_cours":
            case "en cours":
                idStatut = 20;
                break;
            case "traite":
            case "traité":
                idStatut = 30;
                break;
            case "rejete":
            case "rejeté":
                idStatut = 40;
                break;
            default:
                idStatut = 10; // nouveau / en_attente
        }
        
        BigDecimal surface = entity.getSurface() != null ? entity.getSurface() : BigDecimal.ZERO;
        // Estimation du coût par m2 (valeur par défaut si non définie)
        BigDecimal coutParM2 = BigDecimal.valueOf(28750); // Valeur par défaut
        BigDecimal budgetCalcule = surface.multiply(coutParM2);
        
        // Convertir l'ID entreprise de String à Integer si possible
        Integer idEntreprise = null;
        if (entity.getEntrepriseId() != null) {
            try {
                idEntreprise = Integer.parseInt(entity.getEntrepriseId());
            } catch (NumberFormatException e) {
                log.debug("Impossible de convertir entrepriseId en Integer: {}", entity.getEntrepriseId());
            }
        }
        
        // Récupérer les photos depuis le JSON stocké
        List<String> photos = null;
        if (entity.getPhotos() != null && !entity.getPhotos().isEmpty()) {
            try {
                photos = objectMapper.readValue(entity.getPhotos(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Erreur lecture photos JSON: {}", e.getMessage());
            }
        }
        
        return SignalementDTO.builder()
                .id(dtoId)
                .idSignalement(null) // Pas de correspondance dans signalement_details
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .probleme(entity.getProblemeNom())
                .problemeNom(entity.getProblemeNom())
                .dateSignalement(entity.getDateCreationFirebase())
                .dateCreationFirebase(entity.getDateCreationFirebase())
                .surface(surface)
                .coutParM2(coutParM2)
                .idEntreprise(idEntreprise)
                .entrepriseNom(entity.getEntrepriseNom())
                .commentaires(entity.getDescription())
                .budgetEstime(entity.getBudgetEstime() != null ? entity.getBudgetEstime() : entity.getBudget())
                .notesManager(entity.getNotesManager())
                .dateModification(entity.getDateModificationLocal())
                .idStatut(idStatut)
                .statutLibelle(SignalementDTO.getStatutLibelle(idStatut))
                .budgetCalcule(budgetCalcule)
                // Champs d'avancement
                .avancementPourcentage(entity.getAvancementPourcentage() != null ? entity.getAvancementPourcentage() : 0)
                .dateDebutTravaux(entity.getDateDebutTravaux())
                .dateFinTravaux(entity.getDateFinTravaux())
                // Photos
                .photos(photos)
                .build();
    }
    
    /**
     * Convertit un statut en pourcentage d'avancement
     * 10 (EN_ATTENTE) -> 0%
     * 20 (EN_COURS) -> 50%  
     * 30 (TRAITE) -> 100%
     * 40 (REJETE) -> 0%
     */
    private Integer mapStatutToAvancement(Integer idStatut) {
        if (idStatut == null) return null;
        
        switch (idStatut) {
            case 10: // EN_ATTENTE
                return 0;
            case 20: // EN_COURS
                return 50;
            case 30: // TRAITE
                return 100;
            case 40: // REJETE
                return 0;
            default:
                return null; // Ne pas modifier l'avancement pour les statuts inconnus
        }
    }
}