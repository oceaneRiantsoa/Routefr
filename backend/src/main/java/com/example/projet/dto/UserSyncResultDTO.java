package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le résultat de la synchronisation des utilisateurs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncResultDTO {
    
    private boolean success;
    private String message;
    
    // Nombre d'utilisateurs envoyés vers Firebase
    private int pushedToFirebase;
    
    // Nombre d'utilisateurs récupérés de Firebase
    private int pulledFromFirebase;
    
    // Nombre d'erreurs
    private int errors;
    
    // Détails des erreurs
    private List<String> errorDetails;
    
    // Date de synchronisation
    private LocalDateTime syncDate;
}
