package com.example.projet.controller;

import com.example.projet.dto.EntrepriseDTO;
import com.example.projet.dto.SignalementDTO;
import com.example.projet.dto.SignalementUpdateDTO;
import com.example.projet.service.SignalementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/signalements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manager - Signalements", description = "Gestion des signalements par le manager")
public class SignalementController {

    private final SignalementService signalementService;

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
}
