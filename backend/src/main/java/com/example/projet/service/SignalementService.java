package com.example.projet.service;

import com.example.projet.dto.EntrepriseDTO;
import com.example.projet.dto.SignalementDTO;
import com.example.projet.dto.SignalementUpdateDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
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

    private final SignalementFirebaseRepository firebaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Récupère tous les signalements (uniquement depuis signalement_firebase)
     */
    public List<SignalementDTO> getAllSignalements() {
        List<SignalementFirebase> signalements = firebaseRepository.findAllByOrderByDateCreationFirebaseDesc();
        List<SignalementDTO> dtos = signalements.stream()
                .map(this::mapFirebaseToDTO)
                .collect(Collectors.toList());
        log.info("📋 Total signalements: {}", dtos.size());
        return dtos;
    }

    /**
     * Récupère les signalements filtrés par statut
     */
    public List<SignalementDTO> getSignalementsByStatut(Integer idStatut) {
        // Convertir le code statut en avancement
        Integer avancement = mapStatutToAvancement(idStatut);
        List<SignalementFirebase> signalements = firebaseRepository
                .findByAvancementPourcentageOrderByDateCreationFirebaseDesc(avancement);
        return signalements.stream()
                .map(this::mapFirebaseToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un signalement par son ID
     */
    public Optional<SignalementDTO> getSignalementById(Long id) {
        return firebaseRepository.findById(id)
                .map(this::mapFirebaseToDTO);
    }

    /**
     * Met à jour un signalement directement dans signalement_firebase
     * et marque needsFirebaseSync = true pour synchronisation ultérieure
     */
    @Transactional
    public SignalementDTO updateSignalement(Long id, SignalementUpdateDTO updateDTO) {
        SignalementFirebase entity = firebaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'ID: " + id));

        // Mise à jour des champs modifiables par le manager
        if (updateDTO.getSurface() != null) {
            entity.setSurface(updateDTO.getSurface());
        }
        if (updateDTO.getBudgetEstime() != null) {
            entity.setBudgetEstime(updateDTO.getBudgetEstime());
        }
        if (updateDTO.getIdEntreprise() != null) {
            entity.setEntrepriseId(String.valueOf(updateDTO.getIdEntreprise()));
        }
        if (updateDTO.getNotesManager() != null) {
            entity.setNotesManager(updateDTO.getNotesManager());
        }

        // Mise à jour du statut et de l'avancement
        if (updateDTO.getIdStatut() != null) {
            Integer avancement = mapStatutToAvancement(updateDTO.getIdStatut());
            entity.setAvancementPourcentage(avancement);
            entity.setStatutLocal(getStatutCodeFromId(updateDTO.getIdStatut()));
            entity.setStatus(getStatutCodeFromId(updateDTO.getIdStatut()));

            LocalDateTime now = LocalDateTime.now();
            if (updateDTO.getIdStatut() == 20 && entity.getDateDebutTravaux() == null) {
                entity.setDateDebutTravaux(now);
            }
            if (updateDTO.getIdStatut() == 30) {
                entity.setDateFinTravaux(now);
                if (entity.getDateDebutTravaux() == null) {
                    entity.setDateDebutTravaux(now);
                }
            }
            if (updateDTO.getIdStatut() == 10 || updateDTO.getIdStatut() == 40) {
                entity.setDateDebutTravaux(null);
                entity.setDateFinTravaux(null);
            }
        }

        // Marquer pour synchronisation vers Firebase
        entity.setDateModificationLocal(LocalDateTime.now());
        entity.setNeedsFirebaseSync(true);

        firebaseRepository.save(entity);
        log.info("✅ Signalement {} mis à jour (needsFirebaseSync=true)", id);

        return mapFirebaseToDTO(entity);
    }

    /**
     * Récupère les statistiques par statut
     */
    public Map<String, Long> getStatistiquesByStatut() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("EN_ATTENTE", 0L);
        stats.put("EN_COURS", 0L);
        stats.put("TRAITE", 0L);
        stats.put("REJETE", 0L);

        List<Object[]> results = firebaseRepository.countByAvancementGrouped();
        for (Object[] row : results) {
            Integer avancement = row[0] != null ? ((Number) row[0]).intValue() : 0;
            Long count = ((Number) row[1]).longValue();
            String code = mapAvancementToStatutCode(avancement);
            stats.put(code, stats.getOrDefault(code, 0L) + count);
        }

        // Compter les rejetés séparément
        List<SignalementFirebase> all = firebaseRepository.findAll();
        long rejetes = all.stream()
                .filter(s -> "rejete".equalsIgnoreCase(s.getStatus()) || "rejeté".equalsIgnoreCase(s.getStatus()))
                .count();
        if (rejetes > 0) {
            stats.put("REJETE", rejetes);
            // Soustraire des EN_ATTENTE car avancement=0 pour les rejetés aussi
            stats.put("EN_ATTENTE", Math.max(0, stats.get("EN_ATTENTE") - rejetes));
        }

        return stats;
    }

    /**
     * Récupère la liste des entreprises disponibles
     */
    public List<EntrepriseDTO> getAllEntreprises() {
        // On garde cette méthode pour compatibilité - sera remplacé par un appel direct
        // au repository entreprise si besoin
        return List.of();
    }

    /**
     * Convertit une entité SignalementFirebase en DTO
     */
    private SignalementDTO mapFirebaseToDTO(SignalementFirebase entity) {
        Integer idStatut = mapAvancementToStatutId(entity);

        BigDecimal surface = entity.getSurface() != null ? entity.getSurface() : BigDecimal.ZERO;
        BigDecimal coutParM2 = BigDecimal.valueOf(28750);
        BigDecimal budgetCalcule = surface.multiply(coutParM2);

        Integer idEntreprise = null;
        if (entity.getEntrepriseId() != null) {
            try {
                idEntreprise = Integer.parseInt(entity.getEntrepriseId());
            } catch (NumberFormatException e) {
                log.debug("Impossible de convertir entrepriseId: {}", entity.getEntrepriseId());
            }
        }

        List<String> photos = null;
        if (entity.getPhotos() != null && !entity.getPhotos().isEmpty()) {
            try {
                photos = objectMapper.readValue(entity.getPhotos(), new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                log.warn("Erreur lecture photos JSON: {}", e.getMessage());
            }
        }

        return SignalementDTO.builder()
                .id(entity.getId())
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
                // Always compute avancement from the statut; ignore any metadata/numeric
                // avancement stored in Firebase
                .avancementPourcentage(mapStatutToAvancement(idStatut))
                .dateDebutTravaux(entity.getDateDebutTravaux())
                .dateFinTravaux(entity.getDateFinTravaux())
                .photos(photos)
                .build();
    }

    /**
     * Détermine le code statut à partir de l'entité
     */
    private Integer mapAvancementToStatutId(SignalementFirebase entity) {
        String status = entity.getStatus() != null ? entity.getStatus().toLowerCase() : "";

        // Mapper directement le statut Firebase
        if ("terminate".equals(status) || "termine".equals(status) || "terminé".equals(status))
            return 30;
        if ("en_cours".equals(status) || "en cours".equals(status))
            return 20;
        if ("rejete".equals(status) || "rejeté".equals(status))
            return 40;
        if ("nouveau".equals(status))
            return 10;

        // Ne pas prendre en compte les champs metadata/avancementPourcentage.
        // Se baser uniquement sur le champ `status` fourni par Firebase.
        return 10; // statut inconnu -> nouveau
    }

    private String mapAvancementToStatutCode(Integer avancement) {
        if (avancement == null)
            return "EN_ATTENTE";
        switch (avancement) {
            case 50:
                return "EN_COURS";
            case 100:
                return "TRAITE";
            default:
                return "EN_ATTENTE";
        }
    }

    private Integer mapStatutToAvancement(Integer idStatut) {
        if (idStatut == null)
            return 0;
        switch (idStatut) {
            case 20:
                return 50;
            case 30:
                return 100;
            default:
                return 0;
        }
    }

    private String getStatutCodeFromId(Integer idStatut) {
        if (idStatut == null)
            return "nouveau";
        switch (idStatut) {
            case 20:
                return "en_cours";
            case 30:
                return "termine";
            case 40:
                return "rejete";
            default:
                return "nouveau";
        }
    }
}