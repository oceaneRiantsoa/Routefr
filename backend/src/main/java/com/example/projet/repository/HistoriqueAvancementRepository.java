package com.example.projet.repository;

import com.example.projet.entity.HistoriqueAvancement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueAvancementRepository extends JpaRepository<HistoriqueAvancement, Long> {
    
    /**
     * Trouver l'historique d'un signalement local (par ID séquentiel)
     */
    List<HistoriqueAvancement> findBySignalementIdOrderByDateChangementAsc(Long signalementId);
    
    /**
     * Trouver l'historique d'un signalement Firebase
     */
    List<HistoriqueAvancement> findByFirebaseSignalementIdOrderByDateChangementAsc(Long firebaseSignalementId);
    
    /**
     * Trouver tous les passages en "en_cours" (début de travaux)
     */
    @Query("SELECT h FROM HistoriqueAvancement h WHERE h.nouveauStatut IN ('en_cours', 'EN_COURS', '20') ORDER BY h.dateChangement DESC")
    List<HistoriqueAvancement> findAllDebutsTravaux();
    
    /**
     * Trouver tous les passages en "traité/terminé" (fin de travaux)
     */
    @Query("SELECT h FROM HistoriqueAvancement h WHERE h.nouveauStatut IN ('traite', 'termine', 'TRAITE', '30') ORDER BY h.dateChangement DESC")
    List<HistoriqueAvancement> findAllFinsTravaux();
    
    /**
     * Calculer le temps moyen entre "nouveau" et "en_cours" (en secondes)
     * Pour les signalements locaux
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (enc.date_changement - nouv.date_changement)))
        FROM historique_avancement nouv
        JOIN historique_avancement enc ON nouv.signalement_id = enc.signalement_id
        WHERE nouv.nouveau_statut IN ('nouveau', 'EN_ATTENTE', '10')
        AND enc.nouveau_statut IN ('en_cours', 'EN_COURS', '20')
        AND nouv.signalement_id IS NOT NULL
        """, nativeQuery = true)
    Double calculerTempsMoyenPriseEnChargeLocal();
    
    /**
     * Calculer le temps moyen entre "nouveau" et "en_cours" (en secondes)
     * Pour les signalements Firebase
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (enc.date_changement - nouv.date_changement)))
        FROM historique_avancement nouv
        JOIN historique_avancement enc ON nouv.firebase_signalement_id = enc.firebase_signalement_id
        WHERE nouv.nouveau_statut IN ('nouveau', 'EN_ATTENTE', '10')
        AND enc.nouveau_statut IN ('en_cours', 'EN_COURS', '20')
        AND nouv.firebase_signalement_id IS NOT NULL
        """, nativeQuery = true)
    Double calculerTempsMoyenPriseEnChargeFirebase();
    
    /**
     * Calculer le temps moyen entre "en_cours" et "traité" (en secondes)
     * Pour les signalements locaux
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (fin.date_changement - enc.date_changement)))
        FROM historique_avancement enc
        JOIN historique_avancement fin ON enc.signalement_id = fin.signalement_id
        WHERE enc.nouveau_statut IN ('en_cours', 'EN_COURS', '20')
        AND fin.nouveau_statut IN ('traite', 'termine', 'TRAITE', '30')
        AND enc.signalement_id IS NOT NULL
        """, nativeQuery = true)
    Double calculerTempsMoyenTraitementLocal();
    
    /**
     * Calculer le temps moyen entre "en_cours" et "traité" (en secondes)
     * Pour les signalements Firebase
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (fin.date_changement - enc.date_changement)))
        FROM historique_avancement enc
        JOIN historique_avancement fin ON enc.firebase_signalement_id = fin.firebase_signalement_id
        WHERE enc.nouveau_statut IN ('en_cours', 'EN_COURS', '20')
        AND fin.nouveau_statut IN ('traite', 'termine', 'TRAITE', '30')
        AND enc.firebase_signalement_id IS NOT NULL
        """, nativeQuery = true)
    Double calculerTempsMoyenTraitementFirebase();
    
    /**
     * Compter les changements par statut (pour statistiques)
     */
    @Query("SELECT h.nouveauStatut, COUNT(h) FROM HistoriqueAvancement h GROUP BY h.nouveauStatut")
    List<Object[]> countByNouveauStatut();
    
    /**
     * Historique récent (derniers 100 changements)
     */
    @Query("SELECT h FROM HistoriqueAvancement h ORDER BY h.dateChangement DESC")
    List<HistoriqueAvancement> findRecentHistory();
}
