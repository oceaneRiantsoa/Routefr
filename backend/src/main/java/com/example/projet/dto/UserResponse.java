package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String uid;
    private String email;
    private String displayName;
    private String role;
    private boolean emailVerified;
    private boolean accountLocked;
    private int failedAttempts;
    private String createdAt;
}