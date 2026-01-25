package com.example.projet.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementUpdateDTO {
    
    // Surface modifiable par le manager
    private BigDecimal surface;
    
    // Budget estimé par le manager
    private BigDecimal budgetEstime;
    
    // ID de l'entreprise assignée (référence vers table entreprise)
    private Integer idEntreprise;
    
    // Notes du manager
    private String notesManager;
    
    // Nouveau statut (10=En attente, 20=En cours, 30=Traité, 40=Rejeté)
    private Integer idStatut;
}
