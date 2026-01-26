package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le résultat d'une opération de synchronisation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResultDTO {
    
    private boolean success;
    private String message;
    
    // Statistiques de synchronisation
    private int totalFirebase;      // Total de signalements dans Firebase
    private int nouveaux;           // Nouveaux signalements ajoutés
    private int misAJour;           // Signalements mis à jour
    private int ignores;            // Signalements ignorés (déjà existants)
    private int erreurs;            // Erreurs de traitement
    
    private LocalDateTime dateSynchronisation;
    
    // Détails des erreurs éventuelles
    private List<String> erreursDetails;
    
    // Liste des signalements synchronisés (optionnel)
    private List<FirebaseSignalementDTO> signalementsSynchronises;
}
