package com.example.projet.service;

import com.example.projet.dto.SecuritySettingsDTO;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service pour gérer les paramètres de sécurité de l'application
 * Ces paramètres sont modifiables dynamiquement via l'interface Manager
 */
@Service
@Slf4j
public class SecuritySettingsService {

    @Value("${app.session.duration-minutes:60}")
    private int defaultSessionDuration;

    @Value("${app.auth.max-failed-attempts:3}")
    private int defaultMaxFailedAttempts;

    // Paramètres dynamiques (modifiables en runtime)
    @Getter
    private int sessionDurationMinutes;

    @Getter
    private int maxFailedAttempts;

    @PostConstruct
    public void init() {
        // Initialiser avec les valeurs par défaut du fichier de config
        this.sessionDurationMinutes = defaultSessionDuration;
        this.maxFailedAttempts = defaultMaxFailedAttempts;
        log.info("🔧 Paramètres de sécurité initialisés - Session: {} min, Max tentatives: {}", 
                sessionDurationMinutes, maxFailedAttempts);
    }

    /**
     * Récupérer tous les paramètres de sécurité
     */
    public SecuritySettingsDTO getSettings() {
        return SecuritySettingsDTO.builder()
                .sessionDurationMinutes(sessionDurationMinutes)
                .maxFailedAttempts(maxFailedAttempts)
                .build();
    }

    /**
     * Mettre à jour les paramètres de sécurité
     */
    public SecuritySettingsDTO updateSettings(SecuritySettingsDTO newSettings) {
        // Validation
        if (newSettings.getSessionDurationMinutes() < 1) {
            throw new IllegalArgumentException("La durée de session doit être au moins 1 minute");
        }
        if (newSettings.getSessionDurationMinutes() > 1440) { // Max 24h
            throw new IllegalArgumentException("La durée de session ne peut pas dépasser 1440 minutes (24h)");
        }
        if (newSettings.getMaxFailedAttempts() < 1) {
            throw new IllegalArgumentException("Le nombre max de tentatives doit être au moins 1");
        }
        if (newSettings.getMaxFailedAttempts() > 10) {
            throw new IllegalArgumentException("Le nombre max de tentatives ne peut pas dépasser 10");
        }

        // Mise à jour
        this.sessionDurationMinutes = newSettings.getSessionDurationMinutes();
        this.maxFailedAttempts = newSettings.getMaxFailedAttempts();

        log.info("🔧 Paramètres de sécurité mis à jour - Session: {} min, Max tentatives: {}", 
                sessionDurationMinutes, maxFailedAttempts);

        return getSettings();
    }

    /**
     * Réinitialiser les paramètres aux valeurs par défaut
     */
    public SecuritySettingsDTO resetToDefaults() {
        this.sessionDurationMinutes = defaultSessionDuration;
        this.maxFailedAttempts = defaultMaxFailedAttempts;

        log.info("🔄 Paramètres de sécurité réinitialisés aux valeurs par défaut");

        return getSettings();
    }
}
