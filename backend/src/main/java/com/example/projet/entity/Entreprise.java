package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant une entreprise de réparation
 */
@Entity
@Table(name = "entreprise")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entreprise {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nom_entreprise", nullable = false, length = 150)
    private String nomEntreprise;
    
    @Column(name = "localisation", length = 200)
    private String localisation;
    
    @Column(name = "contact", length = 100)
    private String contact;
}
