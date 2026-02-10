package com.example.projet.controller;

import com.example.projet.dto.AvancementDTO;
import com.example.projet.dto.EntrepriseDTO;
import com.example.projet.dto.ProblemeDTO;
import com.example.projet.dto.SignalementDTO;
import com.example.projet.dto.SignalementUpdateDTO;
import com.example.projet.dto.StatistiquesDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.service.SignalementService;
import com.example.projet.service.StatistiquesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/signalements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manager - Signalements", description = "Gestion des signalements par le manager")
public class SignalementController {

    private final SignalementService signalementService;
    private final StatistiquesService statistiquesService;

    /**
     * Liste tous les signalements
     */
    @GetMapping
    @Operation(summary = "Liste tous les signalements", description = "Récupère tous les signalements avec leurs détails")
    public ResponseEntity<List<SignalementDTO>> getAllSignalements() {
        log.info("GET /api/manager/signalements - Récupération de tous les signalements");
        List<SignalementDTO> signalements = signalementService.getAllSignalements();
        return ResponseEntity.ok(signalements);
    }

    /**
     * Récupère un signalement par son ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un signalement", description = "Récupère les détails complets d'un signalement")
    public ResponseEntity<SignalementDTO> getSignalementById(@PathVariable Long id) {
        log.info("GET /api/manager/signalements/{} - Récupération du signalement", id);
        return signalementService.getSignalementById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Met à jour un signalement
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modifier un signalement", description = "Met à jour les informations et le statut d'un signalement")
    public ResponseEntity<SignalementDTO> updateSignalement(
            @PathVariable Long id,
            @RequestBody SignalementUpdateDTO updateDTO) {
        log.info("PUT /api/manager/signalements/{} - Mise à jour du signalement", id);
        try {
            SignalementDTO updated = signalementService.updateSignalement(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Erreur lors de la mise à jour du signalement {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Filtre les signalements par statut (idStatut: 10=En attente, 20=En cours, 30=Traité, 40=Rejeté)
     */
    @GetMapping("/statut/{idStatut}")
    @Operation(summary = "Filtrer par statut", description = "Récupère les signalements ayant un statut spécifique (10, 20, 30 ou 40)")
    public ResponseEntity<List<SignalementDTO>> getSignalementsByStatut(@PathVariable Integer idStatut) {
        log.info("GET /api/manager/signalements/statut/{} - Filtrage par statut", idStatut);
        List<SignalementDTO> signalements = signalementService.getSignalementsByStatut(idStatut);
        return ResponseEntity.ok(signalements);
    }

    /**
     * Récupère les statistiques par statut
     */
    @GetMapping("/statistiques")
    @Operation(summary = "Statistiques par statut", description = "Récupère le nombre de signalements par statut")
    public ResponseEntity<Map<String, Long>> getStatistiques() {
        log.info("GET /api/manager/signalements/statistiques - Récupération des statistiques");
        Map<String, Long> stats = signalementService.getStatistiquesByStatut();
        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère la liste des statuts disponibles
     */
    @GetMapping("/statuts")
    @Operation(summary = "Liste des statuts", description = "Récupère tous les statuts possibles pour un signalement")
    public ResponseEntity<List<Map<String, Object>>> getStatuts() {
        log.info("GET /api/manager/signalements/statuts - Liste des statuts");
        List<Map<String, Object>> statuts = Arrays.asList(
            Map.of("id", 10, "code", "EN_ATTENTE", "libelle", "En attente"),
            Map.of("id", 20, "code", "EN_COURS", "libelle", "En cours"),
            Map.of("id", 30, "code", "TRAITE", "libelle", "Traité"),
            Map.of("id", 40, "code", "REJETE", "libelle", "Rejeté")
        );
        return ResponseEntity.ok(statuts);
    }

    /**
     * Récupère la liste des entreprises disponibles
     */
    @GetMapping("/entreprises")
    @Operation(summary = "Liste des entreprises", description = "Récupère toutes les entreprises disponibles")
    public ResponseEntity<List<EntrepriseDTO>> getEntreprises() {
        log.info("GET /api/manager/signalements/entreprises - Liste des entreprises");
        List<EntrepriseDTO> entreprises = signalementService.getAllEntreprises();
        return ResponseEntity.ok(entreprises);
    }

    // ==================== NOUVEAUX ENDPOINTS AVANCEMENT ====================

    /**
     * Récupère les statistiques complètes avec délais de traitement
     */
    @GetMapping("/statistiques/completes")
    @Operation(summary = "Statistiques complètes", description = "Récupère les statistiques détaillées avec délais moyens de traitement")
    public ResponseEntity<StatistiquesDTO> getStatistiquesCompletes() {
        log.info("GET /api/manager/signalements/statistiques/completes - Statistiques complètes");
        StatistiquesDTO stats = statistiquesService.getStatistiquesCompletes();
        return ResponseEntity.ok(stats);
    }

    /**
     * Met à jour l'avancement d'un signalement (0%, 50%, 100%)
     */
    @PutMapping("/{id}/avancement")
    @Operation(summary = "Modifier l'avancement", description = "Met à jour le pourcentage d'avancement et enregistre les dates")
    public ResponseEntity<?> updateAvancement(
            @PathVariable Long id,
            @RequestBody AvancementDTO avancementDTO) {
        log.info("PUT /api/manager/signalements/{}/avancement - Mise à jour avancement: {}", id, avancementDTO);
        try {
            if (id >= 10000L) {
                // Signalement Firebase - soustraire l'offset pour obtenir l'ID réel en BDD
                Long dbId = id - 10000L;
                SignalementFirebase updated = statistiquesService.updateAvancement(dbId, avancementDTO);
                
                // Utiliser HashMap car Map.of() n'accepte pas les valeurs null
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Avancement mis à jour");
                response.put("id", updated.getId());
                response.put("avancement", updated.getAvancementPourcentage());
                response.put("status", updated.getStatus());
                response.put("dateDebutTravaux", updated.getDateDebutTravaux() != null ? updated.getDateDebutTravaux().toString() : null);
                response.put("dateFinTravaux", updated.getDateFinTravaux() != null ? updated.getDateFinTravaux().toString() : null);
                return ResponseEntity.ok(response);
            } else {
                // Signalement local - convertir l'avancement en mise à jour de statut
                SignalementUpdateDTO updateDTO = new SignalementUpdateDTO();
                String statut = avancementDTO.getStatut().toLowerCase();
                if (statut.contains("nouveau") || statut.equals("non_traite")) {
                    updateDTO.setIdStatut(10);
                } else if (statut.contains("cours")) {
                    updateDTO.setIdStatut(20);
                } else if (statut.contains("termin") || statut.equals("traite")) {
                    updateDTO.setIdStatut(30);
                } else if (statut.contains("rejet")) {
                    updateDTO.setIdStatut(40);
                } else {
                    updateDTO.setIdStatut(10);
                }
                SignalementDTO updated = signalementService.updateSignalement(id, updateDTO);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Avancement mis à jour");
                response.put("id", updated.getId());
                response.put("avancement", updated.getAvancementPourcentage());
                response.put("status", updated.getStatutLibelle());
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            log.error("Erreur lors de la mise à jour de l'avancement: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Récupère les statuts d'avancement disponibles
     */
    @GetMapping("/avancements")
    @Operation(summary = "Liste des avancements", description = "Récupère tous les statuts d'avancement possibles")
    public ResponseEntity<List<Map<String, Object>>> getAvancements() {
        log.info("GET /api/manager/signalements/avancements - Liste des avancements");
        List<Map<String, Object>> avancements = Arrays.asList(
            Map.of("pourcentage", 0, "statut", "nouveau", "libelle", "Nouveau", "color", "#f39c12", "icon", "🟡"),
            Map.of("pourcentage", 50, "statut", "en_cours", "libelle", "En cours", "color", "#3498db", "icon", "🔵"),
            Map.of("pourcentage", 100, "statut", "termine", "libelle", "Terminé", "color", "#27ae60", "icon", "🟢")
        );
        return ResponseEntity.ok(avancements);
    }

    // ==================== ENDPOINTS GESTION DES PRIX PAR PROBLÈME ====================

    /**
     * Récupère la liste des types de problèmes avec leurs prix par m²
     */
    @GetMapping("/problemes")
    @Operation(summary = "Liste des problèmes", description = "Récupère tous les types de problèmes avec leur prix par m² en Ariary")
    public ResponseEntity<List<ProblemeDTO>> getProblemes() {
        log.info("GET /api/manager/signalements/problemes - Liste des types de problèmes");
        List<ProblemeDTO> problemes = signalementService.getAllProblemes();
        return ResponseEntity.ok(problemes);
    }

    /**
     * Met à jour le prix par m² d'un type de problème
     */
    @PutMapping("/problemes/{id}/prix")
    @Operation(summary = "Modifier le prix par m²", description = "Met à jour le prix par m² d'un type de problème en Ariary")
    public ResponseEntity<?> updatePrixProbleme(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("PUT /api/manager/signalements/problemes/{}/prix - Mise à jour du prix", id);
        try {
            BigDecimal nouveauPrix = new BigDecimal(body.get("coutParM2").toString());
            ProblemeDTO updated = signalementService.updatePrixProbleme(id, nouveauPrix);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Prix mis à jour",
                "probleme", updated
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du prix: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Récupère les niveaux de réparation disponibles (1 à 10)
     */
    @GetMapping("/niveaux")
    @Operation(summary = "Liste des niveaux", description = "Récupère les niveaux de réparation disponibles (1 à 10)")
    public ResponseEntity<List<Map<String, Object>>> getNiveauxReparation() {
        log.info("GET /api/manager/signalements/niveaux - Liste des niveaux de réparation");
        List<Map<String, Object>> niveaux = Arrays.asList(
            Map.of("niveau", 1, "libelle", "Niveau 1 - Très léger", "multiplicateur", 1),
            Map.of("niveau", 2, "libelle", "Niveau 2 - Léger", "multiplicateur", 2),
            Map.of("niveau", 3, "libelle", "Niveau 3 - Modéré", "multiplicateur", 3),
            Map.of("niveau", 4, "libelle", "Niveau 4 - Significatif", "multiplicateur", 4),
            Map.of("niveau", 5, "libelle", "Niveau 5 - Moyen", "multiplicateur", 5),
            Map.of("niveau", 6, "libelle", "Niveau 6 - Important", "multiplicateur", 6),
            Map.of("niveau", 7, "libelle", "Niveau 7 - Grave", "multiplicateur", 7),
            Map.of("niveau", 8, "libelle", "Niveau 8 - Très grave", "multiplicateur", 8),
            Map.of("niveau", 9, "libelle", "Niveau 9 - Critique", "multiplicateur", 9),
            Map.of("niveau", 10, "libelle", "Niveau 10 - Urgent", "multiplicateur", 10)
        );
        return ResponseEntity.ok(niveaux);
    }
}
