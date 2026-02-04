package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO pour les statistiques des signalements avec délais de traitement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiquesDTO {
    
    // Compteurs par statut
    private Long nombreTotal;
    private Long nombreNouveau;      // 0%
    private Long nombreEnCours;      // 50%
    private Long nombreTermine;      // 100%
    private Long nombreRejete;
    
    // Délais de traitement (en jours)
    private Double delaiMoyenTraitement;        // Moyenne globale (création -> fin)
    private Double delaiMoyenDebutTravaux;      // Création -> début travaux
    private Double delaiMoyenFinTravaux;        // Début -> fin travaux
    
    // Délais par type de problème
    private Map<String, Double> delaiParType;
    
    // Pourcentages
    private Double pourcentageNouveau;
    private Double pourcentageEnCours;
    private Double pourcentageTermine;
    private Double pourcentageRejete;
}
