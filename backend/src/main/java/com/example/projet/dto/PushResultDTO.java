package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le résultat d'envoi des données vers Firebase
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushResultDTO {
    
    private boolean success;
    private String message;
    
    // Statistiques
    private int totalEnvoyes;
    private int nouveaux;
    private int misAJour;
    private int erreurs;
    
    // Détails
    private List<String> erreursDetails;
    private List<String> signalementsEnvoyes;
    
    private LocalDateTime datePush;
}
