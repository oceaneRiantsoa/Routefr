package com.example.projet.service;


import com.example.projet.entity.*;
import com.example.projet.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    
    private final LocalUserRepository userRepository;
    private final SecuritySettingsService securitySettings;
    
    // Règle métier: Limite de tentatives
    @Transactional
    public void checkLoginAttempts(String email) {
        Optional<LocalUser> userOpt = userRepository.findByEmail(email);
        int maxFailedAttempts = securitySettings.getMaxFailedAttempts();
        
        if (userOpt.isPresent()) {
            LocalUser user = userOpt.get();
            
            if (user.isAccountLocked()) {
                throw new RuntimeException("Compte bloqué. Contactez un administrateur.");
            }
            
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
        int maxFailedAttempts = securitySettings.getMaxFailedAttempts();
        
        if (userOpt.isPresent()) {
            LocalUser user = userOpt.get();
            int newAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(newAttempts);
            
            log.warn("⚠️ Tentative échouée {} pour {}", newAttempts, email);
            
            // Bloquer immédiatement si limite atteinte
            if (newAttempts >= maxFailedAttempts) {
                user.setAccountLocked(true);
                log.error("🔒 Compte {} bloqué après {} tentatives", email, maxFailedAttempts);
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
}