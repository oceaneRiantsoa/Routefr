package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour envoyer un signalement vers Firebase (format mobile)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementPushDTO {
    
    // Identifiants
    private String id;                  // ID Firebase ou généré
    private Long localId;               // ID local PostgreSQL
    
    // Position
    private Double latitude;
    private Double longitude;
    
    // Informations du signalement
    private String problemeId;
    private String problemeNom;
    private String description;
    
    // Statut et gestion
    private String status;              // nouveau, en_cours, traite, rejete
    private String statutLibelle;       // En attente, En cours, Traité, Rejeté
    
    // Données financières
    private BigDecimal surface;
    private BigDecimal budget;
    private BigDecimal budgetEstime;
    private BigDecimal coutParM2;
    
    // Entreprise assignée
    private String entrepriseId;
    private String entrepriseNom;
    
    // Notes et commentaires
    private String notesManager;
    private String commentaires;
    
    // Dates (en timestamp millisecondes pour Firebase)
    private Long dateCreation;
    private Long dateModification;
    private Long datePush;
    
    // Informations utilisateur (si disponible)
    private String userId;
    private String userEmail;
    
    // Photo
    private String photoUrl;
    
    // Source des données
    private String source;              // "local" ou "firebase"
    
    // Pour affichage mobile
    private String couleur;             // Couleur selon le statut
    private String icone;               // Icône selon le type de problème
}
