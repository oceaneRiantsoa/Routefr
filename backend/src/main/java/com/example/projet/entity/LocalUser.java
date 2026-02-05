package com.example.projet.entity;
// package com.projet.cloud.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "local_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = true)
    private String firebaseUid;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String displayName;
    private String role;
    
    private int failedAttempts;
    private boolean accountLocked;
    
    private String passwordHash;
    
    // Mot de passe en clair temporaire pour synchronisation vers Firebase
    @Column(name = "password_plain_temp")
    private String passwordPlainTemp;
    
    // Indicateur de synchronisation avec Firebase
    @Column(name = "synced_to_firebase")
    @Builder.Default
    private Boolean syncedToFirebase = false;
    
    // Date de dernière synchronisation
    @Column(name = "firebase_sync_date")
    private LocalDateTime firebaseSyncDate;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}