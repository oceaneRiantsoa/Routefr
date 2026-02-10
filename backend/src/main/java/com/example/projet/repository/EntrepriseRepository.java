package com.example.projet.repository;

import com.example.projet.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les entreprises de réparation
 */
@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    
    /**
     * Recherche les entreprises par nom
     */
    List<Entreprise> findByNomEntrepriseContainingIgnoreCase(String nom);
    
    /**
     * Recherche les entreprises par localisation
     */
    List<Entreprise> findByLocalisationContainingIgnoreCase(String localisation);
}
