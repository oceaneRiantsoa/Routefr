package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les types de problèmes avec leur prix par m²
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemeDTO {
    
    private Long id;
    private String nom;
    private String detail;
    private BigDecimal coutParM2;  // Prix par m² en Ariary
    
    /**
     * Format d'affichage du prix en Ariary
     */
    public String getCoutParM2Formate() {
        if (coutParM2 == null) return "0 Ar";
        return String.format("%,.0f Ar", coutParM2);
    }
}
