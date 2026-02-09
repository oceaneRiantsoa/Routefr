package com.example.projet.service;

import com.example.projet.dto.PointDetailDTO;
import com.example.projet.dto.RecapDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementDetailsRepository;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final SignalementFirebaseRepository signalementFirebaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parser les photos depuis JSON
     */
    private List<String> parsePhotos(String photosJson) {
        if (photosJson == null || photosJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(photosJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Erreur parsing photos: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupérer tous les points pour affichage sur la carte
     * Combine les signalements locaux ET les signalements synchronisés depuis Firebase
     */
    public List<PointDetailDTO> getAllPoints() {
        log.info("Récupération de tous les points pour la carte");

        List<PointDetailDTO> allPoints = new ArrayList<>();

        // 1. Récupérer les signalements de la table signalement_details (ancienne source)
        List<Object[]> rawData = signalementRepository.findAllPointsWithDetails();
        List<PointDetailDTO> localPoints = rawData.stream()
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
        
        allPoints.addAll(localPoints);
        log.info("Points locaux (signalement_details): {}", localPoints.size());

        // 2. Récupérer les signalements synchronisés depuis Firebase
        List<SignalementFirebase> firebaseSignalements = signalementFirebaseRepository.findAll();
        List<PointDetailDTO> firebasePoints = firebaseSignalements.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    // Convertir le statut Firebase en idStatut local
                    Integer idStatut = convertFirebaseStatusToId(s.getStatus());
                    
                    // Utiliser un ID négatif ou offset pour distinguer des signalements locaux
                    Long id = s.getId() != null ? s.getId() + 10000L : null;
                    
                    double surface = s.getSurface() != null ? s.getSurface().doubleValue() : 0.0;
                    double budget = s.getBudget() != null ? s.getBudget().doubleValue() : 0.0;
                    
                    // Parser les photos depuis JSON
                    List<String> photos = parsePhotos(s.getPhotos());
                    
                    PointDetailDTO dto = new PointDetailDTO(
                            id,
                            s.getLatitude(),
                            s.getLongitude(),
                            s.getProblemeNom() != null ? s.getProblemeNom() : "Signalement Firebase",
                            s.getDateCreationFirebase(),
                            idStatut,
                            surface,
                            0.0, // coutParM2 non utilisé, on met le budget directement
                            s.getEntrepriseNom(),
                            s.getDescription()
                    );
                    dto.setBudget(budget);
                    dto.setPhotos(photos);
                    return dto;
                })
                .collect(Collectors.toList());
        
        allPoints.addAll(firebasePoints);
        log.info("Points Firebase (signalement_firebase): {}", firebasePoints.size());
        log.info("Total des points pour la carte: {}", allPoints.size());

        return allPoints;
    }

    /**
     * Convertir le statut Firebase en ID de statut local
     */
    private Integer convertFirebaseStatusToId(String firebaseStatus) {
        if (firebaseStatus == null) return 10; // EN_ATTENTE par défaut
        
        switch (firebaseStatus.toLowerCase()) {
            case "nouveau":
            case "en_attente":
                return 10; // EN_ATTENTE
            case "en_cours":
                return 20; // EN_COURS
            case "traite":
            case "resolu":
                return 30; // TRAITE
            case "rejete":
                return 40; // REJETE
            default:
                return 10; // EN_ATTENTE par défaut
        }
    }

    /**
     * Calculer le récapitulatif global (incluant Firebase)
     */
    public RecapDTO getRecap() {
        log.info("Calcul du récapitulatif");

        // Récapitulatif des signalements locaux
        List<Object[]> result = signalementRepository.getRecapitulation();

        int nbPointsLocal = 0;
        double totalSurfaceLocal = 0.0;
        double avancementLocal = 0.0;
        double totalBudgetLocal = 0.0;

        if (!result.isEmpty() && result.get(0) != null) {
            Object[] row = result.get(0);
            nbPointsLocal = row[0] != null ? ((Number) row[0]).intValue() : 0;
            totalSurfaceLocal = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            avancementLocal = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            totalBudgetLocal = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        }

        // Récapitulatif des signalements Firebase
        List<SignalementFirebase> firebaseSignalements = signalementFirebaseRepository.findAll();
        int nbPointsFirebase = firebaseSignalements.size();
        double totalSurfaceFirebase = firebaseSignalements.stream()
                .filter(s -> s.getSurface() != null)
                .mapToDouble(s -> s.getSurface().doubleValue())
                .sum();
        double totalBudgetFirebase = firebaseSignalements.stream()
                .filter(s -> s.getBudget() != null)
                .mapToDouble(s -> s.getBudget().doubleValue())
                .sum();
        
        // Calculer l'avancement Firebase (signalements traités / total)
        long firebaseTraites = firebaseSignalements.stream()
                .filter(s -> "traite".equalsIgnoreCase(s.getStatus()) || "resolu".equalsIgnoreCase(s.getStatus()))
                .count();

        // Combiner les deux sources
        int totalPoints = nbPointsLocal + nbPointsFirebase;
        double totalSurface = totalSurfaceLocal + totalSurfaceFirebase;
        double totalBudget = totalBudgetLocal + totalBudgetFirebase;
        
        // Calculer l'avancement global
        double avancementGlobal = 0.0;
        if (totalPoints > 0) {
            // Nombre total de signalements traités
            long localTraites = (long) (nbPointsLocal * avancementLocal / 100.0);
            long totalTraites = localTraites + firebaseTraites;
            avancementGlobal = (double) totalTraites / totalPoints * 100.0;
        }

        RecapDTO recap = new RecapDTO(totalPoints, totalSurface, avancementGlobal, totalBudget);
        log.info("Récapitulatif global: {} points ({} local + {} Firebase), {} m², {}% avancement, {} Ar",
                recap.nbPoints, nbPointsLocal, nbPointsFirebase, recap.totalSurface, recap.avancementPourcent, recap.totalBudget);

        return recap;
    }
}
