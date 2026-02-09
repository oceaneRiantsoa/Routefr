package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour les signalements synchronisés depuis Firebase
 */
@Entity
@Table(name = "signalement_firebase")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementFirebase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_id", unique = true, nullable = false)
    private String firebaseId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "probleme_id")
    private String problemeId;

    @Column(name = "probleme_nom")
    private String problemeNom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "surface")
    private BigDecimal surface;

    @Column(name = "budget")
    private BigDecimal budget;

    @Column(name = "date_creation_firebase")
    private LocalDateTime dateCreationFirebase;

    @Column(name = "photo_url")
    private String photoUrl;

    // Photos base64 stockées comme JSON array
    @Column(name = "photos", columnDefinition = "TEXT")
    private String photos;

    // Champs pour la gestion locale
    @Column(name = "entreprise_id")
    private String entrepriseId;

    @Column(name = "entreprise_nom")
    private String entrepriseNom;

    @Column(name = "notes_manager", columnDefinition = "TEXT")
    private String notesManager;

    @Column(name = "statut_local")
    private String statutLocal;

    @Column(name = "budget_estime")
    private BigDecimal budgetEstime;

    @Column(name = "date_synchronisation")
    private LocalDateTime dateSynchronisation;

    @Column(name = "date_modification_local")
    private LocalDateTime dateModificationLocal;

    // Marqueur pour synchroniser les modifications vers Firebase
    @Column(name = "needs_firebase_sync")
    @Builder.Default
    private Boolean needsFirebaseSync = false;

    // Champs pour le suivi d'avancement
    @Column(name = "avancement_pourcentage")
    @Builder.Default
    private Integer avancementPourcentage = 0;

    @Column(name = "date_debut_travaux")
    private LocalDateTime dateDebutTravaux;

    @Column(name = "date_fin_travaux")
    private LocalDateTime dateFinTravaux;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point geom;
}
