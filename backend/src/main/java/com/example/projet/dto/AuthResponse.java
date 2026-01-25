package com.example.projet.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private boolean success;
    private UserResponse user;
    private String firebaseToken; // ID Token
    private String customToken; // Pour client SDK
}