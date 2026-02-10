package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "signalement_id")
    private Long signalementId;

    @Column(name = "firebase_signalement_id")
    private Long firebaseSignalementId;

    @Column(name = "ancien_statut", length = 50)
    private String ancienStatut;

    @Column(name = "nouveau_statut", length = 50)
    private String nouveauStatut;

    @Column(name = "ancien_avancement")
    private Integer ancienAvancement;

    @Column(name = "nouveau_avancement")
    private Integer nouveauAvancement;

    @Column(name = "date_changement")
    private LocalDateTime dateChangement;

    @Column(name = "utilisateur_id")
    private String utilisateurId;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;
}
