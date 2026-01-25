package com.example.projet.service;

import com.example.projet.dto.EntrepriseDTO;
import com.example.projet.dto.SignalementDTO;
import com.example.projet.dto.SignalementUpdateDTO;
import com.example.projet.entity.SignalementDetails;
import com.example.projet.entity.SignalementStatus;
import com.example.projet.repository.SignalementDetailsRepository;
import com.example.projet.repository.SignalementStatusRepository;
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
    private final SignalementStatusRepository statusRepository;

    /**
     * Récupère tous les signalements pour le manager
     */
    public List<SignalementDTO> getAllSignalements() {
        List<Object[]> results = repository.findAllSignalementsForManager();
        return results.stream()
                .map(this::mapToSignalementDTO)
                .collect(Collectors.toList());
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
     */
    @Transactional
    public SignalementDTO updateSignalement(Long id, SignalementUpdateDTO updateDTO) {
        SignalementDetails entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signalement non trouvé avec l'ID: " + id));

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
        }

        log.info("Signalement {} mis à jour avec statut: {}", id, updateDTO.getIdStatut());

        // Retourner le signalement mis à jour
        return getSignalementById(id).orElseThrow();
    }

    /**
     * Récupère les statistiques par statut
     */
    public Map<String, Long> getStatistiquesByStatut() {
        List<Object[]> results = repository.countByStatut();
        Map<String, Long> stats = new LinkedHashMap<>();
        
        // Initialiser tous les statuts à 0
        stats.put("EN_ATTENTE", 0L);
        stats.put("EN_COURS", 0L);
        stats.put("TRAITE", 0L);
        stats.put("REJETE", 0L);
        
        // Remplir avec les valeurs réelles
        for (Object[] row : results) {
            Integer idStatut = row[0] != null ? ((Number) row[0]).intValue() : 10;
            Long count = ((Number) row[1]).longValue();
            String code = SignalementDTO.getStatutCode(idStatut);
            stats.put(code, stats.getOrDefault(code, 0L) + count);
        }
        
        return stats;
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
                .build();
    }
}
