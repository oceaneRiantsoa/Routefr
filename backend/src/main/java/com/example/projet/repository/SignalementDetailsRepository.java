package com.example.projet.repository;

import com.example.projet.entity.SignalementDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignalementDetailsRepository extends JpaRepository<SignalementDetails, Long> {

    @Query(value = """
        SELECT 
            sd.id,
            COALESCE(ST_Y(CAST(sd.geom AS geometry)), sd.latitude) as lat,
            COALESCE(ST_X(CAST(sd.geom AS geometry)), sd.longitude) as lng,
            p.nom as probleme,
            s.datetime_signalement,
            ss.idStatut,
            sd.surface,
            p.cout_par_m2,
            e.nom_entreprise,
            sd.commentaires
        FROM signalement_details sd
        JOIN signalement s ON s.id = sd.id_signalement
        JOIN probleme p ON p.id = sd.id_probleme
        LEFT JOIN entreprise e ON e.id = sd.id_entreprise
        LEFT JOIN signalement_status ss ON ss.id_signalement = s.id
        """, nativeQuery = true)
    List<Object[]> findAllPointsWithDetails();

    @Query(value = """
        SELECT 
            COUNT(DISTINCT sd.id) as nb_points,
            COALESCE(SUM(sd.surface), 0) as total_surface,
            COALESCE(
                ROUND(
                    (COUNT(DISTINCT CASE WHEN ss.idStatut = 30 THEN sd.id END) * 100.0) / 
                    NULLIF(COUNT(DISTINCT sd.id), 0)
                , 2), 0
            ) as avancement_pourcent,
            COALESCE(SUM(sd.surface * p.cout_par_m2), 0) as total_budget
        FROM signalement_details sd
        JOIN probleme p ON p.id = sd.id_probleme
        LEFT JOIN signalement_status ss ON ss.id_signalement = sd.id_signalement
        """, nativeQuery = true)
    List<Object[]> getRecapitulation();

    // Requête pour obtenir tous les signalements avec leurs détails complets pour le Manager
    @Query(value = """
        SELECT 
            sd.id,
            sd.id_signalement,
            COALESCE(ST_Y(CAST(sd.geom AS geometry)), sd.latitude) as lat,
            COALESCE(ST_X(CAST(sd.geom AS geometry)), sd.longitude) as lng,
            p.nom as probleme,
            s.datetime_signalement,
            sd.surface,
            p.cout_par_m2,
            sd.id_entreprise,
            e.nom_entreprise,
            sd.commentaires,
            sd.budget_estime,
            sd.notes_manager,
            sd.date_modification,
            COALESCE(ss.idStatut, 10) as id_statut
        FROM signalement_details sd
        JOIN signalement s ON s.id = sd.id_signalement
        JOIN probleme p ON p.id = sd.id_probleme
        LEFT JOIN entreprise e ON e.id = sd.id_entreprise
        LEFT JOIN signalement_status ss ON ss.id_signalement = s.id
        ORDER BY s.datetime_signalement DESC
        """, nativeQuery = true)
    List<Object[]> findAllSignalementsForManager();

    // Requête pour filtrer par statut
    @Query(value = """
        SELECT 
            sd.id,
            sd.id_signalement,
            COALESCE(ST_Y(CAST(sd.geom AS geometry)), sd.latitude) as lat,
            COALESCE(ST_X(CAST(sd.geom AS geometry)), sd.longitude) as lng,
            p.nom as probleme,
            s.datetime_signalement,
            sd.surface,
            p.cout_par_m2,
            sd.id_entreprise,
            e.nom_entreprise,
            sd.commentaires,
            sd.budget_estime,
            sd.notes_manager,
            sd.date_modification,
            COALESCE(ss.idStatut, 10) as id_statut
        FROM signalement_details sd
        JOIN signalement s ON s.id = sd.id_signalement
        JOIN probleme p ON p.id = sd.id_probleme
        LEFT JOIN entreprise e ON e.id = sd.id_entreprise
        LEFT JOIN signalement_status ss ON ss.id_signalement = s.id
        WHERE COALESCE(ss.idStatut, 10) = :idStatut
        ORDER BY s.datetime_signalement DESC
        """, nativeQuery = true)
    List<Object[]> findAllSignalementsForManagerByStatut(@Param("idStatut") Integer idStatut);

    // Compter par statut pour les statistiques
    @Query(value = """
        SELECT 
            COALESCE(ss.idStatut, 10) as statut,
            COUNT(*) as count
        FROM signalement_details sd
        LEFT JOIN signalement_status ss ON ss.id_signalement = sd.id_signalement
        GROUP BY COALESCE(ss.idStatut, 10)
        """, nativeQuery = true)
    List<Object[]> countByStatut();

    // Liste des entreprises disponibles
    @Query(value = "SELECT id, nom_entreprise, localisation, contact FROM entreprise ORDER BY nom_entreprise", nativeQuery = true)
    List<Object[]> findAllEntreprises();
}