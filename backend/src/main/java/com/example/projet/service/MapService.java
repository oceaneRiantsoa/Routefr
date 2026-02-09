package com.example.projet.service;

import com.example.projet.dto.PointDetailDTO;
import com.example.projet.dto.RecapDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des données de la carte (partie visiteur)
 * Utilise uniquement la table signalement_firebase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapService {

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
     * Utilise uniquement la table signalement_firebase
     */
    public List<PointDetailDTO> getAllPoints() {
        log.info("Récupération de tous les points pour la carte");

        List<SignalementFirebase> firebaseSignalements = signalementFirebaseRepository.findAll();
        List<PointDetailDTO> allPoints = firebaseSignalements.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    // Déterminer le statut à partir de avancementPourcentage
                    Integer idStatut = convertAvancementToStatutId(s);
                    
                    double surface = s.getSurface() != null ? s.getSurface().doubleValue() : 0.0;
                    double budget = s.getBudget() != null ? s.getBudget().doubleValue() : 0.0;
                    
                    // Parser les photos depuis JSON
                    List<String> photos = parsePhotos(s.getPhotos());
                    
                    PointDetailDTO dto = new PointDetailDTO(
                            s.getId(),
                            s.getLatitude(),
                            s.getLongitude(),
                            s.getProblemeNom() != null ? s.getProblemeNom() : "Signalement",
                            s.getDateCreationFirebase(),
                            idStatut,
                            surface,
                            0.0,
                            s.getEntrepriseNom(),
                            s.getDescription()
                    );
                    dto.setBudget(budget);
                    dto.setPhotos(photos);
                    return dto;
                })
                .collect(Collectors.toList());
        
        log.info("Total des points pour la carte: {}", allPoints.size());
        return allPoints;
    }

    /**
     * Convertir avancementPourcentage + status en idStatut
     */
    private Integer convertAvancementToStatutId(SignalementFirebase s) {
        // Vérifier d'abord le statut rejeté
        String status = s.getStatus() != null ? s.getStatus().toLowerCase() : "";
        if ("rejete".equals(status) || "rejeté".equals(status)) return 40;
        
        // Utiliser avancementPourcentage pour les autres
        Integer avancement = s.getAvancementPourcentage() != null ? s.getAvancementPourcentage() : 0;
        switch (avancement) {
            case 50: return 20;  // EN_COURS
            case 100: return 30; // TRAITE
            default: return 10;  // EN_ATTENTE
        }
    }

    /**
     * Calculer le récapitulatif global (uniquement depuis signalement_firebase)
     * Utilise avancementPourcentage pour le calcul de l'avancement moyen
     */
    public RecapDTO getRecap() {
        log.info("Calcul du récapitulatif");

        List<SignalementFirebase> signalements = signalementFirebaseRepository.findAll();
        
        int nbPoints = signalements.size();
        double totalSurface = signalements.stream()
                .filter(s -> s.getSurface() != null)
                .mapToDouble(s -> s.getSurface().doubleValue())
                .sum();
        double totalBudget = signalements.stream()
                .filter(s -> s.getBudget() != null)
                .mapToDouble(s -> s.getBudget().doubleValue())
                .sum();
        
        // Calculer l'avancement moyen basé sur avancementPourcentage
        double avancementGlobal = 0.0;
        if (nbPoints > 0) {
            double sommeAvancement = signalements.stream()
                    .mapToDouble(s -> s.getAvancementPourcentage() != null ? s.getAvancementPourcentage() : 0)
                    .sum();
            avancementGlobal = sommeAvancement / nbPoints;
        }

        RecapDTO recap = new RecapDTO(nbPoints, totalSurface, avancementGlobal, totalBudget);
        log.info("Récapitulatif: {} points, {} m², {}% avancement, {} Ar",
                recap.nbPoints, recap.totalSurface, recap.avancementPourcent, recap.totalBudget);

        return recap;
    }
}
