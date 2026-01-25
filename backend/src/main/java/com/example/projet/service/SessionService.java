package com.example.projet.service;

import com.example.projet.entity.UserSession;
import com.example.projet.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final UserSessionRepository sessionRepository;
    
    @Value("${app.session.duration-minutes:60}")
    private int sessionDurationMinutes;
    
    /**
     * Créer une nouvelle session pour un utilisateur
     */
    @Transactional
    public String createSession(String firebaseUid, String ipAddress, String userAgent) {
        String sessionToken = UUID.randomUUID().toString();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(sessionDurationMinutes);
        
        UserSession session = UserSession.builder()
            .firebaseUid(firebaseUid)
            .sessionToken(sessionToken)
            .createdAt(now)
            .expiresAt(expiresAt)
            .active(true)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        
        sessionRepository.save(session);
        log.info("Session créée pour {} (expire dans {} min)", firebaseUid, sessionDurationMinutes);
        
        return sessionToken;
    }
    
    /**
     * Vérifier si une session est valide
     */
    public boolean isSessionValid(String sessionToken) {
        Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        UserSession session = sessionOpt.get();
        
        if (!session.isActive()) {
            return false;
        }
        
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            invalidateSession(sessionToken);
            return false;
        }
        
        return true;
    }
    
    /**
     * Prolonger une session (refresh)
     */
    @Transactional
    public void refreshSession(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionDurationMinutes));
            sessionRepository.save(session);
            log.info("Session {} prolongée", sessionToken);
        });
    }
    
    /**
     * Invalider une session (logout)
     */
    @Transactional
    public void invalidateSession(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
            log.info("Session {} invalidée", sessionToken);
        });
    }
    
    /**
     * Invalider toutes les sessions d'un utilisateur
     */
    @Transactional
    public void invalidateAllUserSessions(String firebaseUid) {
        List<UserSession> sessions = sessionRepository.findByFirebaseUidAndActiveTrue(firebaseUid);
        sessions.forEach(session -> session.setActive(false));
        sessionRepository.saveAll(sessions);
        log.info("Toutes les sessions de {} invalidées", firebaseUid);
    }
    
    /**
     * Récupérer les sessions actives d'un utilisateur
     */
    public List<UserSession> getUserActiveSessions(String firebaseUid) {
        return sessionRepository.findByFirebaseUidAndActiveTrue(firebaseUid);
    }
    
    /**
     * Nettoyage automatique des sessions expirées (chaque heure)
     */
    @Scheduled(fixedRate = 3600000) // 1 heure
    @Transactional
    public void cleanExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Sessions expirées nettoyées");
    }
    
    /**
     * Obtenir la session par token
     */
    public Optional<UserSession> getSession(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken);
    }
}
