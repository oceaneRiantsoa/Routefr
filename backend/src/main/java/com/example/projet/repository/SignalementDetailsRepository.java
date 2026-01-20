package com.example.projet.repository;

import com.example.projet.entity.SignalementDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SignalementDetailsRepository extends JpaRepository<SignalementDetails, Long> {

    @Query(value = """
        SELECT 
            sd.id,
            ST_Y(CAST(sd.geom AS geometry)) as lat,
            ST_X(CAST(sd.geom AS geometry)) as lng,
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
}