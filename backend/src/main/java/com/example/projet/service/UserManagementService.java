package com.example.projet.service;


import com.example.projet.entity.*;
import com.example.projet.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    
    private final LocalUserRepository userRepository;
    // private final SecuritySettingsService securitySettingsService;
    
    // Règle métier: Limite de tentatives - maintenant dynamique depuis la base de données
    private final SecuritySettingsService securitySettings;
    
    // Règle métier: Limite de tentatives
    @Transactional
    public void checkLoginAttempts(String email) {
        Optional<LocalUser> userOpt = userRepository.findByEmail(email);
        int maxFailedAttempts = securitySettings.getMaxLoginAttempts();
        
        if (userOpt.isPresent()) {
            LocalUser user = userOpt.get();
            
            if (user.isAccountLocked()) {
                throw new RuntimeException("Compte bloqué. Contactez un administrateur.");
            }
            
            // Vérification : si déjà à la limite, bloquer
            if (user.getFailedAttempts() >= maxFailedAttempts) {
                user.setAccountLocked(true);
                userRepository.save(user);
                throw new RuntimeException("Compte bloqué après " + maxFailedAttempts + " tentatives.");
            }
        }
    }
    
    @Transactional
    public void incrementFailedAttempts(String email) {
        Optional<LocalUser> userOpt = userRepository.findByEmail(email);
        int maxFailedAttempts = securitySettings.getMaxLoginAttempts();
        
        if (userOpt.isPresent()) {
            LocalUser user = userOpt.get();
            int newAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(newAttempts);
            
            log.warn("⚠️ Tentative échouée {} pour {} (limite: {})", newAttempts, email, maxFailedAttempts);
            
            // Bloquer dès que la limite est atteinte (pas >=, mais ==)
            if (newAttempts == maxFailedAttempts) {
                user.setAccountLocked(true);
                log.error("🔒 Compte {} bloqué après {} tentatives", email, newAttempts);
            }
            
            userRepository.save(user);
            
        } else {
            // Si l'utilisateur n'existe pas dans local_users, le créer
            log.warn("⚠️ Utilisateur {} n'existe pas dans local_users, création automatique", email);
            
            LocalUser newUser = LocalUser.builder()
                .firebaseUid("unknown") // On ne connaît pas l'UID ici
                .email(email)
                .displayName(null)
                .role("USER")
                .failedAttempts(1) // Première tentative échouée
                .accountLocked(false)
                .createdAt(LocalDateTime.now())
                .build();
            
            userRepository.save(newUser);
            log.info("✅ Utilisateur local créé avec 1 tentative échouée: {}", email);
        }
    }
    
    @Transactional
    public void resetFailedAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            userRepository.save(user);
        });
    }
    
    @Transactional
    public void createLocalUser(String uid, String email, String displayName) {
        LocalUser user = LocalUser.builder()
            .firebaseUid(uid)
            .email(email)
            .displayName(displayName)
            .role("USER")
            .failedAttempts(0)
            .accountLocked(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        userRepository.save(user);
        log.info("Utilisateur local créé pour: {}", email);
    }
    
    /**
     * Créer un utilisateur local avec le hash du mot de passe (lors de l'inscription)
     */
    public void createLocalUserWithPassword(String firebaseUid, String email, String displayName, String passwordHash) {
        LocalUser user = new LocalUser();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordHash);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        
        userRepository.save(user);
        log.info("Utilisateur local créé avec mot de passe hashé: {}", email);
    }
    
    /**
     * Synchronise un utilisateur Firebase vers local_users.
     * Si l'utilisateur existe déjà, met à jour ses infos.
     * Si l'utilisateur n'existe pas, le crée.
     * IMPORTANT: Cette méthode doit être appelée AVANT de créer une session.
     */
    @Transactional
    public void syncFirebaseUserToLocal(String firebaseUid, String email, String displayName, String passwordHash) {
        Optional<LocalUser> existingUser = userRepository.findByFirebaseUid(firebaseUid);
        
        LocalUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            // Mettre à jour le hash si fourni
            if (passwordHash != null) {
                user.setPasswordHash(passwordHash);
            }
        } else {
            user = new LocalUser();
            user.setFirebaseUid(firebaseUid);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setPasswordHash(passwordHash);
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());
        }
        
        userRepository.save(user);
        log.info("Utilisateur synchronisé avec la base locale: {}", email);
    }
    
    @Transactional
    public void syncFirebaseUserToLocal(String uid, String email, String displayName) {
        Optional<LocalUser> existingByUid = userRepository.findByFirebaseUid(uid);
        
        if (existingByUid.isPresent()) {
            // L'utilisateur existe déjà par UID, mettre à jour si nécessaire
            LocalUser user = existingByUid.get();
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
            }
            if (displayName != null && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
            }
            userRepository.save(user);
            log.info("✅ Utilisateur local synchronisé (existant par UID): {}", email);
            return;
        }
        
        // Vérifier si l'utilisateur existe par email (créé lors d'une tentative échouée)
        Optional<LocalUser> existingByEmail = userRepository.findByEmail(email);
        
        if (existingByEmail.isPresent()) {
            // Mettre à jour l'UID Firebase
            LocalUser user = existingByEmail.get();
            user.setFirebaseUid(uid);
            if (displayName != null) {
                user.setDisplayName(displayName);
            }
            userRepository.save(user);
            log.info("✅ Utilisateur local mis à jour avec UID Firebase: {}", email);
            return;
        }
        
        // L'utilisateur n'existe pas, le créer
        LocalUser newUser = LocalUser.builder()
            .firebaseUid(uid)
            .email(email)
            .displayName(displayName)
            .role("USER")
            .failedAttempts(0)
            .accountLocked(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        userRepository.save(newUser);
        log.info("✅ Nouvel utilisateur local créé lors de la synchronisation: {}", email);
    }
    
    @Transactional
    public void updateLocalUser(String uid, Map<String, Object> updates) {
        Optional<LocalUser> userOpt = userRepository.findByFirebaseUid(uid);
        
        if (userOpt.isPresent()) {
            LocalUser user = userOpt.get();
            
            if (updates.containsKey("displayName")) {
                user.setDisplayName((String) updates.get("displayName"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
            }
            
            userRepository.save(user);
        }
    }
    
    public void disableLocalUser(String uid) {
        userRepository.findByFirebaseUid(uid).ifPresent(user -> {
            user.setAccountLocked(true);
            userRepository.save(user);
        });
    }
    
    public void enableLocalUser(String uid) {
        userRepository.findByFirebaseUid(uid).ifPresent(user -> {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            userRepository.save(user);
        });
    }
    
    public Optional<LocalUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional
    public LocalUser save(LocalUser user) {
        return userRepository.save(user);
    }

    // ==================== NOUVELLES MÉTHODES POUR AUTH LOCALE ====================

    /**
     * Créer un utilisateur uniquement en local (sans Firebase)
     * Le mot de passe en clair est stocké temporairement pour la future sync Firebase
     */
    @Transactional
    public LocalUser createLocalUserOnly(String email, String displayName, String passwordHash, String passwordPlain, String role) {
        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }
        
        // Générer un UID local temporaire (sera remplacé par l'UID Firebase lors de la sync)
        String localUid = "local-" + UUID.randomUUID().toString();
        
        LocalUser user = LocalUser.builder()
            .firebaseUid(localUid)
            .email(email)
            .displayName(displayName)
            .passwordHash(passwordHash)
            .passwordPlainTemp(passwordPlain) // Stocké temporairement pour sync Firebase
            .role(role != null ? role : "USER")
            .failedAttempts(0)
            .accountLocked(false)
            .syncedToFirebase(false) // Pas encore synchronisé
            .createdAt(LocalDateTime.now())
            .build();
        
        LocalUser savedUser = userRepository.save(user);
        log.info("✅ Utilisateur local créé (non synchronisé): {} avec rôle {}", email, role);
        
        return savedUser;
    }

    /**
     * Récupère tous les utilisateurs non synchronisés avec Firebase
     */
    public List<LocalUser> getAllUsersNotSynced() {
        return userRepository.findBySyncedToFirebaseFalse();
    }

    /**
     * Récupère le nombre d'utilisateurs non synchronisés
     */
    public long countUsersNotSynced() {
        return userRepository.countBySyncedToFirebaseFalse();
    }

    /**
     * Marquer un utilisateur comme synchronisé avec Firebase
     */
    @Transactional
    public void markUserAsSynced(Long userId, String firebaseUid) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFirebaseUid(firebaseUid);
            user.setSyncedToFirebase(true);
            user.setFirebaseSyncDate(LocalDateTime.now());
            user.setPasswordPlainTemp(null); // Effacer le mot de passe en clair après sync
            userRepository.save(user);
            log.info("✅ Utilisateur {} marqué comme synchronisé avec Firebase UID: {}", user.getEmail(), firebaseUid);
        });
    }

    /**
     * Authentification locale uniquement (sans Firebase)
     */
    public Optional<LocalUser> authenticateLocally(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Récupère tous les utilisateurs locaux
     */
    public List<LocalUser> getAllLocalUsers() {
        return userRepository.findAll();
    }

    /**
     * Trouver un utilisateur par Firebase UID
     */
    public Optional<LocalUser> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }
}