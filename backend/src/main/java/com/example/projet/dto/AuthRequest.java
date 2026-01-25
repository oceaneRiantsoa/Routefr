package com.example.projet.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class AuthRequest {
    
    @NotBlank(message = "Email est obligatoire")
    @Email(message = "Email invalide")
    private String email;
    
    @NotBlank(message = "Mot de passe est obligatoire")
    @Size(min = 6, message = "Mot de passe doit avoir au moins 6 caractères")
    private String password;
    
    private String displayName; // Pour inscription
}