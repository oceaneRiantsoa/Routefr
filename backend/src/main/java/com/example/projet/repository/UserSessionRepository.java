package com.example.projet.repository;

import com.example.projet.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByFirebaseUid(String firebaseUid);
    
    List<UserSession> findByFirebaseUidAndActiveTrue(String firebaseUid);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    void deleteByFirebaseUid(String firebaseUid);
}
