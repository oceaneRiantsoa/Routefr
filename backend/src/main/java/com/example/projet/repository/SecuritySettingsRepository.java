package com.example.projet.repository;

import com.example.projet.entity.SecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les paramètres de sécurité
 */
@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {
    
    /**
     * Récupère le premier enregistrement de paramètres de sécurité
     */
    default Optional<SecuritySettings> findSettings() {
        return findAll().stream().findFirst();
    }
}
