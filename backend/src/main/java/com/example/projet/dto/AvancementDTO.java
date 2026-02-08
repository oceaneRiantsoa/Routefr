package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour de l'avancement d'un signalement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvancementDTO {
    
    // Statut cible: "nouveau", "en_cours", "termine"
    private String statut;
    
    // Pourcentage correspondant (0, 50, 100)
    private Integer pourcentage;
    
    // Notes optionnelles
    private String notes;
}
