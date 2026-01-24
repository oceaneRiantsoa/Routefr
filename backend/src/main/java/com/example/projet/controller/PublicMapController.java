package com.example.projet.controller;

import com.example.projet.dto.PointDetailDTO;
import com.example.projet.dto.RecapDTO;
import com.example.projet.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller public pour les visiteurs (sans authentification)
 * Permet de consulter la carte et les statistiques
 */
@RestController
@RequestMapping("/api/public/map")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Carte Publique", description = "API publique pour les visiteurs - Consultation de la carte")
public class PublicMapController {

    private final MapService mapService;

    /**
     * Récupérer tous les points de signalement pour affichage sur la carte
     * Accessible aux visiteurs sans authentification
     */
    @GetMapping("/points")
    @Operation(summary = "Liste des points de signalement", description = "Récupère tous les points de signalement avec leurs détails (problème, statut, surface, budget, entreprise)")
    @ApiResponse(responseCode = "200", description = "Liste des points récupérée avec succès")
    public ResponseEntity<List<PointDetailDTO>> getPoints() {
        log.info("GET /api/public/map/points - Récupération des points");
        List<PointDetailDTO> points = mapService.getAllPoints();
        log.info("Nombre de points retournés: {}", points.size());
        return ResponseEntity.ok(points);
    }

    /**
     * Récupérer le récapitulatif global
     * Accessible aux visiteurs sans authentification
     */
    @GetMapping("/recap")
    @Operation(summary = "Récapitulatif global", description = "Récupère les statistiques globales: nombre de points, surface totale, avancement, budget total")
    @ApiResponse(responseCode = "200", description = "Récapitulatif calculé avec succès")
    public ResponseEntity<RecapDTO> getRecap() {
        log.info("GET /api/public/map/recap - Calcul du récapitulatif");
        RecapDTO recap = mapService.getRecap();
        return ResponseEntity.ok(recap);
    }

    /**
     * Récupérer les détails d'un point spécifique par son ID
     */
    @GetMapping("/points/{id}")
    @Operation(summary = "Détails d'un point", description = "Récupère les détails complets d'un point de signalement par son ID")
    @ApiResponse(responseCode = "200", description = "Point trouvé")
    @ApiResponse(responseCode = "404", description = "Point non trouvé")
    public ResponseEntity<PointDetailDTO> getPointById(@PathVariable Long id) {
        log.info("GET /api/public/map/points/{} - Récupération d'un point", id);

        List<PointDetailDTO> points = mapService.getAllPoints();
        return points.stream()
                .filter(p -> p.id.equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
