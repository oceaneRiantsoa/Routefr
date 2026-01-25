package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signalement_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_signalement")
    private Integer idSignalement;

    @Column(name = "id_probleme")
    private Integer idProbleme;

    @Column(name = "surface")
    private BigDecimal surface;

    @Column(name = "id_entreprise")
    private Integer idEntreprise;

    @Column(name = "commentaires")
    private String commentaires;

    @Column(columnDefinition = "geography(Point,4326)")
    private Point geom;

    // Champs pour la gestion Manager
    @Column(name = "budget_estime")
    private BigDecimal budgetEstime;

    @Column(name = "entreprise_assignee")
    private String entrepriseAssignee;

    @Column(name = "notes_manager")
    private String notesManager;

    @Column(name = "statut_manager")
    private String statutManager;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}