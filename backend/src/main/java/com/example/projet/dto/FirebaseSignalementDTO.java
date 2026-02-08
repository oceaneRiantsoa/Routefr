package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mapper les signalements depuis Firebase Firestore
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirebaseSignalementDTO {
    
    private String id;              // ID Firebase du document
    private String odId;            // ID du document (même que id)
    private String userId;          // ID utilisateur Firebase
    private String userEmail;       // Email de l'utilisateur
    private Double latitude;
    private Double longitude;
    private String problemeId;      // ex: "nid_poule"
    private String problemeNom;     // ex: "Nid de poule"
    private String description;     // Commentaires/description
    private String status;          // "nouveau", "en_cours", "traite"
    private BigDecimal surface;
    private BigDecimal budget;
    private Long dateCreation;      // Timestamp en millisecondes
    
    // Champs optionnels
    private String entrepriseId;
    private String entrepriseNom;
    private String photoUrl;
}
