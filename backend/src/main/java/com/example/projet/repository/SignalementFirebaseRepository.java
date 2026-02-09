package com.example.projet.repository;

import com.example.projet.entity.SignalementFirebase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignalementFirebaseRepository extends JpaRepository<SignalementFirebase, Long> {
    
    /**
     * Trouver un signalement par son ID Firebase
     */
    Optional<SignalementFirebase> findByFirebaseId(String firebaseId);
    
    /**
     * Vérifier si un signalement existe par son ID Firebase
     */
    boolean existsByFirebaseId(String firebaseId);
    
    /**
     * Trouver tous les signalements d'un utilisateur Firebase
     */
    List<SignalementFirebase> findByUserIdOrderByDateCreationFirebaseDesc(String userId);
    
    /**
     * Trouver tous les signalements par statut
     */
    List<SignalementFirebase> findByStatusOrderByDateCreationFirebaseDesc(String status);
    
    /**
     * Trouver tous les signalements par statut local (côté manager)
     */
    List<SignalementFirebase> findByStatutLocalOrderByDateCreationFirebaseDesc(String statutLocal);
    
    /**
     * Compter les signalements par statut
     */
    long countByStatus(String status);
    
    /**
     * Récupérer tous les signalements ordonnés par date de création décroissante
     */
    List<SignalementFirebase> findAllByOrderByDateCreationFirebaseDesc();
    
    /**
     * Récupérer tous les signalements avec un type de problème spécifique
     */
    List<SignalementFirebase> findByProblemeIdOrderByDateCreationFirebaseDesc(String problemeId);
    
    /**
     * Récupérer les statistiques par statut
     */
    @Query("SELECT s.status, COUNT(s) FROM SignalementFirebase s GROUP BY s.status")
    List<Object[]> countByStatusGrouped();

    // ========================
    // NOUVELLES REQUÊTES POUR LA GESTION UNIFIÉE
    // ========================

    /**
     * Trouver tous les signalements modifiés localement qui doivent être synchronisés
     */
    List<SignalementFirebase> findByNeedsFirebaseSyncTrue();

    /**
     * Filtrer par avancement (0=nouveau, 50=en_cours, 100=terminé)
     */
    List<SignalementFirebase> findByAvancementPourcentageOrderByDateCreationFirebaseDesc(Integer avancementPourcentage);

    /**
     * Compter les signalements par avancement
     */
    @Query("SELECT s.avancementPourcentage, COUNT(s) FROM SignalementFirebase s GROUP BY s.avancementPourcentage")
    List<Object[]> countByAvancementGrouped();

    /**
     * Compter les signalements qui nécessitent une synchronisation Firebase
     */
    long countByNeedsFirebaseSyncTrue();
}
