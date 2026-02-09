package com.example.projet.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementDTO {
    
    private Long id;
    private Integer idSignalement;
    
    // Coordonnées
    private Double latitude;
    private Double longitude;
    
    // Infos du signalement original
    private String probleme;
    private BigDecimal surface;
    private String commentaires;
    private LocalDateTime dateSignalement;
    
    // Infos entreprise (depuis la table entreprise via id_entreprise)
    private Integer idEntreprise;
    private String entrepriseNom;
    
    // Statut (depuis la table signalement_status via idStatut)
    // 10 = En attente, 20 = En cours, 30 = Traité, 40 = Rejeté
    private Integer idStatut;
    private String statutLibelle;
    
    // Infos gérées par le Manager
    private BigDecimal budgetEstime;
    private String notesManager;
    private LocalDateTime dateModification;
    
    // Champs d'avancement
    private Integer avancementPourcentage;
    private LocalDateTime dateCreationFirebase;
    private LocalDateTime dateDebutTravaux;
    private LocalDateTime dateFinTravaux;
    private String problemeNom;
    
    // Budget calculé (surface * cout_par_m2)
    private BigDecimal budgetCalcule;
    private BigDecimal coutParM2;
    
    // Photos base64
    private List<String> photos;
    
    // Helper pour convertir idStatut en libellé
    public static String getStatutLibelle(Integer idStatut) {
        if (idStatut == null) return "En attente";
        switch (idStatut) {
            case 10: return "En attente";
            case 20: return "En cours";
            case 30: return "Traité";
            case 40: return "Rejeté";
            default: return "Inconnu";
        }
    }
    
    // Helper pour convertir idStatut en code
    public static String getStatutCode(Integer idStatut) {
        if (idStatut == null) return "EN_ATTENTE";
        switch (idStatut) {
            case 10: return "EN_ATTENTE";
            case 20: return "EN_COURS";
            case 30: return "TRAITE";
            case 40: return "REJETE";
            default: return "EN_ATTENTE";
        }
    }
}
