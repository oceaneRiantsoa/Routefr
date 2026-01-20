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
    
    @Column(unique = true, nullable = false)
    private String firebaseUid;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String displayName;
    private String role;
    
    private int failedAttempts;
    private boolean accountLocked;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}