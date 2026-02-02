package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour stocker les paramètres de sécurité
 * Table avec une seule ligne (singleton pattern)
 */
@Entity
@Table(name = "security_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Durée de vie des sessions en minutes */
    @Column(name = "session_duration")
    @Builder.Default
    private Integer sessionDuration = 30;
    
    /** Nombre maximum de tentatives de connexion avant blocage */
    @Column(name = "max_login_attempts")
    @Builder.Default
    private Integer maxLoginAttempts = 5;
    
    /** Durée de blocage automatique en minutes */
    @Column(name = "lockout_duration")
    @Builder.Default
    private Integer lockoutDuration = 15;
    
    /** Message de blocage personnalisé */
    @Column(name = "lockout_message")
    @Builder.Default
    private String lockoutMessage = "Votre compte a été temporairement bloqué suite à plusieurs tentatives de connexion échouées.";
    
    /** Activer/désactiver le blocage automatique */
    @Column(name = "auto_lock_enabled")
    @Builder.Default
    private Boolean autoLockEnabled = true;
    
    /** Date de dernière modification */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
