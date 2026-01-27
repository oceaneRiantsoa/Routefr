package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les paramètres de sécurité configurables
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettingsDTO {
    
    private int sessionDurationMinutes;  // Durée de vie des sessions en minutes
    private int maxFailedAttempts;       // Nombre max de tentatives avant blocage
}
