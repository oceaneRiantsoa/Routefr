package com.example.projet.controller;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/sync")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SyncController {

    private final SyncService syncService;
    private final SignalementFirebaseRepository signalementFirebaseRepository;

    /**
     * Synchroniser les signalements depuis Firebase vers PostgreSQL
     * POST /api/manager/sync/pull
     */
    @PostMapping("/pull")
    public ResponseEntity<SyncResultDTO> syncFromFirebase() {
        log.info("📥 Requête de synchronisation depuis Firebase");
        try {
            SyncResultDTO result = syncService.syncSignalementsFromFirebase();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(SyncResultDTO.builder()
                            .success(false)
                            .message("Erreur: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Aperçu des signalements Firebase sans les sauvegarder
     * GET /api/manager/sync/preview
     */
    @GetMapping("/preview")
    public ResponseEntity<?> previewFirebaseSignalements() {
        log.info("👀 Requête d'aperçu des signalements Firebase");
        try {
            List<FirebaseSignalementDTO> signalements = syncService.previewSignalementsFromFirebase();
            return ResponseEntity.ok(signalements);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aperçu: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur Firebase: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Obtenir le statut de synchronisation actuel
     * GET /api/manager/sync/status
     */
    @GetMapping("/status")
    public ResponseEntity<SyncResultDTO> getSyncStatus() {
        log.info("📊 Requête de statut de synchronisation");
        try {
            SyncResultDTO status = syncService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du statut: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Récupérer tous les signalements synchronisés
     * GET /api/manager/sync/signalements
     */
    @GetMapping("/signalements")
    public ResponseEntity<List<SignalementFirebase>> getAllSyncedSignalements() {
        log.info("📋 Requête des signalements synchronisés");
        try {
            List<SignalementFirebase> signalements = signalementFirebaseRepository.findAllByOrderByDateCreationFirebaseDesc();
            return ResponseEntity.ok(signalements);
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Récupérer un signalement par son ID Firebase
     * GET /api/manager/sync/signalements/{firebaseId}
     */
    @GetMapping("/signalements/{firebaseId}")
    public ResponseEntity<SignalementFirebase> getSignalementByFirebaseId(@PathVariable String firebaseId) {
        log.info("🔍 Requête signalement Firebase ID: {}", firebaseId);
        return signalementFirebaseRepository.findByFirebaseId(firebaseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtenir les statistiques des signalements synchronisés
     * GET /api/manager/sync/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("📈 Requête des statistiques de synchronisation");
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total des signalements synchronisés
            long total = signalementFirebaseRepository.count();
            stats.put("total", total);
            
            // Comptage par statut
            long nouveaux = signalementFirebaseRepository.countByStatus("nouveau");
            long enCours = signalementFirebaseRepository.countByStatus("en_cours");
            long traites = signalementFirebaseRepository.countByStatus("traite");
            
            stats.put("nouveaux", nouveaux);
            stats.put("enCours", enCours);
            stats.put("traites", traites);
            
            // Statistiques groupées
            List<Object[]> statsByStatus = signalementFirebaseRepository.countByStatusGrouped();
            Map<String, Long> parStatut = new HashMap<>();
            for (Object[] row : statsByStatus) {
                String status = (String) row[0];
                Long count = (Long) row[1];
                if (status != null) {
                    parStatut.put(status, count);
                }
            }
            stats.put("parStatut", parStatut);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des stats: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
