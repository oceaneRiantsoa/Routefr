package com.example.projet.service;

import com.example.projet.dto.SecuritySettingsDTO;
import com.example.projet.entity.SecuritySettings;
import com.example.projet.repository.SecuritySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour gérer les paramètres de sécurité
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsService {
    
    private final SecuritySettingsRepository repository;
    
    /**
     * Récupère les paramètres de sécurité actuels
     * Si aucun paramètre n'existe, crée les paramètres par défaut
     */
    @Transactional(readOnly = true)
    public SecuritySettingsDTO getSettings() {
        SecuritySettings settings = repository.findSettings()
                .orElseGet(this::createDefaultSettings);
        
        return mapToDTO(settings);
    }
    
    /**
     * Met à jour les paramètres de sécurité
     */
    @Transactional
    public SecuritySettingsDTO updateSettings(SecuritySettingsDTO dto) {
        log.info("Mise à jour des paramètres de sécurité: {}", dto);
        
        // Validation
        if (dto.getSessionDuration() != null) {
            if (dto.getSessionDuration() < 1) {
                throw new IllegalArgumentException("La durée de session doit être au moins 1 minute");
            }
            if (dto.getSessionDuration() > 1440) { // Max 24h
                throw new IllegalArgumentException("La durée de session ne peut pas dépasser 1440 minutes (24h)");
            }
        }
        if (dto.getMaxLoginAttempts() != null) {
            if (dto.getMaxLoginAttempts() < 1) {
                throw new IllegalArgumentException("Le nombre max de tentatives doit être au moins 1");
            }
            if (dto.getMaxLoginAttempts() > 10) {
                throw new IllegalArgumentException("Le nombre max de tentatives ne peut pas dépasser 10");
            }
        }
        
        SecuritySettings settings = repository.findSettings()
                .orElseGet(this::createDefaultSettings);
        
        // Mise à jour des champs
        if (dto.getSessionDuration() != null) {
            settings.setSessionDuration(dto.getSessionDuration());
        }
        if (dto.getMaxLoginAttempts() != null) {
            settings.setMaxLoginAttempts(dto.getMaxLoginAttempts());
        }
        if (dto.getLockoutDuration() != null) {
            settings.setLockoutDuration(dto.getLockoutDuration());
        }
        if (dto.getLockoutMessage() != null) {
            settings.setLockoutMessage(dto.getLockoutMessage());
        }
        if (dto.getAutoLockEnabled() != null) {
            settings.setAutoLockEnabled(dto.getAutoLockEnabled());
        }
        
        SecuritySettings saved = repository.save(settings);
        log.info("Paramètres de sécurité mis à jour avec succès en base de données");
        
        return mapToDTO(saved);
    }
    
    /**
     * Crée les paramètres par défaut
     */
    @Transactional
    public SecuritySettings createDefaultSettings() {
        log.info("Création des paramètres de sécurité par défaut");
        SecuritySettings settings = SecuritySettings.builder().build();
        return repository.save(settings);
    }
    
    /**
     * Convertit l'entité en DTO
     */
    private SecuritySettingsDTO mapToDTO(SecuritySettings settings) {
        return SecuritySettingsDTO.builder()
                .sessionDuration(settings.getSessionDuration())
                .maxLoginAttempts(settings.getMaxLoginAttempts())
                .lockoutDuration(settings.getLockoutDuration())
                .lockoutMessage(settings.getLockoutMessage())
                .autoLockEnabled(settings.getAutoLockEnabled())
                .build();
    }
    
    /**
     * Récupère le nombre maximum de tentatives de connexion
     */
    public int getMaxLoginAttempts() {
        return getSettings().getMaxLoginAttempts();
    }
    
    /**
     * Récupère la durée de blocage en minutes
     */
    public int getLockoutDuration() {
        return getSettings().getLockoutDuration();
    }
    
    /**
     * Vérifie si le blocage automatique est activé
     */
    public boolean isAutoLockEnabled() {
        return getSettings().getAutoLockEnabled();
    }
}
