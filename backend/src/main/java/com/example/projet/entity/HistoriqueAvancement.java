package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité pour tracer l'historique des changements de statut/avancement
 * Permet de calculer le temps moyen de traitement
 */
@Entity
@Table(name = "historique_avancement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueAvancement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence au signalement local (signalement_details)
    @Column(name = "signalement_id")
    private Long signalementId;

    // Référence au signalement Firebase (signalement_firebase)
    @Column(name = "firebase_signalement_id")
    private Long firebaseSignalementId;

    // Statut avant le changement
    @Column(name = "ancien_statut")
    private String ancienStatut;

    // Nouveau statut après le changement
    @Column(name = "nouveau_statut")
    private String nouveauStatut;

    // Avancement avant le changement (0, 50, 100)
    @Column(name = "ancien_avancement")
    private Integer ancienAvancement;

    // Nouvel avancement après le changement (0, 50, 100)
    @Column(name = "nouveau_avancement")
    private Integer nouveauAvancement;

    // Date du changement
    @Column(name = "date_changement")
    @Builder.Default
    private LocalDateTime dateChangement = LocalDateTime.now();

    // Utilisateur qui a fait le changement (email)
    @Column(name = "utilisateur_id")
    private String utilisateurId;

    // Commentaire/notes optionnelles
    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;
}
