package com.example.projet.repository;

import com.example.projet.entity.HistoriqueAvancement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueAvancementRepository extends JpaRepository<HistoriqueAvancement, Long> {

    List<HistoriqueAvancement> findBySignalementIdOrderByDateChangementAsc(Long signalementId);

    List<HistoriqueAvancement> findByFirebaseSignalementIdOrderByDateChangementAsc(Long firebaseSignalementId);

    /**
     * Temps moyen de prise en charge (nouveau -> en_cours) pour signalements locaux
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (h2.date_changement - h1.date_changement)))
        FROM historique_avancement h1
        JOIN historique_avancement h2 ON h1.signalement_id = h2.signalement_id
        WHERE h1.nouveau_statut = '10' AND h2.nouveau_statut = '20'
        AND h1.signalement_id IS NOT NULL
    """, nativeQuery = true)
    Double calculerTempsMoyenPriseEnChargeLocal();

    /**
     * Temps moyen de prise en charge (nouveau -> en_cours) pour signalements Firebase
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (h2.date_changement - h1.date_changement)))
        FROM historique_avancement h1
        JOIN historique_avancement h2 ON h1.firebase_signalement_id = h2.firebase_signalement_id
        WHERE h1.nouveau_statut = 'nouveau' AND h2.nouveau_statut = 'en_cours'
        AND h1.firebase_signalement_id IS NOT NULL
    """, nativeQuery = true)
    Double calculerTempsMoyenPriseEnChargeFirebase();

    /**
     * Temps moyen de traitement (en_cours -> traite) pour signalements locaux
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (h2.date_changement - h1.date_changement)))
        FROM historique_avancement h1
        JOIN historique_avancement h2 ON h1.signalement_id = h2.signalement_id
        WHERE h1.nouveau_statut = '20' AND h2.nouveau_statut = '30'
        AND h1.signalement_id IS NOT NULL
    """, nativeQuery = true)
    Double calculerTempsMoyenTraitementLocal();

    /**
     * Temps moyen de traitement (en_cours -> traite) pour signalements Firebase
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (h2.date_changement - h1.date_changement)))
        FROM historique_avancement h1
        JOIN historique_avancement h2 ON h1.firebase_signalement_id = h2.firebase_signalement_id
        WHERE h1.nouveau_statut = 'en_cours' AND h2.nouveau_statut = 'traite'
        AND h1.firebase_signalement_id IS NOT NULL
    """, nativeQuery = true)
    Double calculerTempsMoyenTraitementFirebase();

    /**
     * Comptage des changements par nouveau statut
     */
    @Query(value = """
        SELECT nouveau_statut, COUNT(*) 
        FROM historique_avancement 
        GROUP BY nouveau_statut
    """, nativeQuery = true)
    List<Object[]> countByNouveauStatut();
}
