package com.example.projet.service;

import com.example.projet.dto.PointDetailDTO;
import com.example.projet.dto.RecapDTO;
import com.example.projet.repository.SignalementDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des données de la carte (partie visiteur)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapService {

    private final SignalementDetailsRepository signalementRepository;

    /**
     * Récupérer tous les points pour affichage sur la carte
     */
    public List<PointDetailDTO> getAllPoints() {
        log.info("Récupération de tous les points pour la carte");

        List<Object[]> rawData = signalementRepository.findAllPointsWithDetails();

        return rawData.stream()
                .map(row -> {
                    Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                    double lat = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    double lng = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    String probleme = (String) row[3];

                    LocalDateTime dateSignalement = null;
                    if (row[4] != null) {
                        dateSignalement = ((Timestamp) row[4]).toLocalDateTime();
                    }

                    Integer idStatut = row[5] != null ? ((Number) row[5]).intValue() : 10;
                    double surface = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
                    double coutParM2 = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;
                    String entreprise = (String) row[8];
                    String commentaires = (String) row[9];

                    return new PointDetailDTO(id, lat, lng, probleme, dateSignalement,
                            idStatut, surface, coutParM2, entreprise, commentaires);
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculer le récapitulatif global
     */
    public RecapDTO getRecap() {
        log.info("Calcul du récapitulatif");

        List<Object[]> result = signalementRepository.getRecapitulation();

        if (result.isEmpty() || result.get(0) == null) {
            return new RecapDTO(0, 0.0, 0.0, 0.0);
        }

        Object[] row = result.get(0);
        int nbPoints = row[0] != null ? ((Number) row[0]).intValue() : 0;
        double totalSurface = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double avancementPourcent = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        double totalBudget = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

        RecapDTO recap = new RecapDTO(nbPoints, totalSurface, avancementPourcent, totalBudget);
        log.info("Récapitulatif: {} points, {} m², {}% avancement, {} Ar",
                recap.nbPoints, recap.totalSurface, recap.avancementPourcent, recap.totalBudget);

        return recap;
    }
}
