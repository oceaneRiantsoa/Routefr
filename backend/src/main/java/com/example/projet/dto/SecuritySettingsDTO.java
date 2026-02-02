package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les paramètres de sécurité
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettingsDTO {
    
    /** Durée de vie des sessions en minutes */
    private Integer sessionDuration;
    
    /** Nombre maximum de tentatives de connexion avant blocage */
    private Integer maxLoginAttempts;
    
    /** Durée de blocage automatique en minutes */
    private Integer lockoutDuration;
    
    /** Message de blocage personnalisé */
    private String lockoutMessage;
    
    /** Activer/désactiver le blocage automatique */
    private Boolean autoLockEnabled;
}
