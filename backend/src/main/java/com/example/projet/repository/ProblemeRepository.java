package com.example.projet.repository;

import com.example.projet.entity.Probleme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des types de problèmes et leurs prix par m²
 */
@Repository
public interface ProblemeRepository extends JpaRepository<Probleme, Long> {
    
    /**
     * Trouver un problème par son nom (insensible à la casse)
     */
    Optional<Probleme> findByNomIgnoreCase(String nom);
    
    /**
     * Trouver des problèmes par nom contenant (pour correspondance partielle)
     * Retourne une liste car plusieurs problèmes peuvent correspondre
     */
    @Query("SELECT p FROM Probleme p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Probleme> findByNomContainingIgnoreCase(@Param("nom") String nom);
    
    /**
     * Mettre à jour le prix par m² d'un problème
     */
    @Modifying
    @Query("UPDATE Probleme p SET p.coutParM2 = :prix WHERE p.id = :id")
    int updateCoutParM2(@Param("id") Long id, @Param("prix") BigDecimal prix);
}
