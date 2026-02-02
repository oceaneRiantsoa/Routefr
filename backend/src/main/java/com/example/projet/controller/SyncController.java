package com.example.projet.controller;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.PushResultDTO;
import com.example.projet.dto.SignalementPushDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Synchronisation Firebase", description = "API de synchronisation bidirectionnelle avec Firebase")
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

    // ============================================================================
    // PUSH VERS FIREBASE - Envoi des données pour l'affichage mobile
    // ============================================================================
    
    /**
     * Envoyer tous les signalements vers Firebase pour l'affichage mobile
     * POST /api/manager/sync/push
     */
    @PostMapping("/push")
    @Operation(summary = "Envoyer vers Firebase", 
               description = "Envoie tous les signalements (locaux + synchronisés) vers Firebase pour l'affichage sur mobile")
    public ResponseEntity<PushResultDTO> pushAllToFirebase() {
        log.info("📤 Requête d'envoi de tous les signalements vers Firebase");
        try {
            PushResultDTO result = syncService.pushAllSignalementsToFirebase();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi vers Firebase: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(PushResultDTO.builder()
                            .success(false)
                            .message("Erreur: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Envoyer un seul signalement vers Firebase
     * POST /api/manager/sync/push/{signalementId}
     */
    @PostMapping("/push/{signalementId}")
    @Operation(summary = "Envoyer un signalement", 
               description = "Envoie un signalement spécifique vers Firebase")
    public ResponseEntity<PushResultDTO> pushSingleToFirebase(@PathVariable String signalementId) {
        log.info("📤 Requête d'envoi du signalement {} vers Firebase", signalementId);
        try {
            PushResultDTO result = syncService.pushSingleSignalement(signalementId);
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi du signalement {}: {}", signalementId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(PushResultDTO.builder()
                            .success(false)
                            .message("Erreur: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Aperçu des signalements qui seront envoyés vers Firebase
     * GET /api/manager/sync/push/preview
     */
    @GetMapping("/push/preview")
    @Operation(summary = "Aperçu des données à envoyer", 
               description = "Prévisualise les signalements qui seront envoyés vers Firebase sans les envoyer")
    public ResponseEntity<List<SignalementPushDTO>> previewPushData() {
        log.info("👀 Requête d'aperçu des données à envoyer vers Firebase");
        try {
            List<SignalementPushDTO> preview = syncService.previewSignalementsForPush();
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aperçu: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Statistiques complètes de synchronisation (pull + push)
     * GET /api/manager/sync/stats/full
     */
    @GetMapping("/stats/full")
    @Operation(summary = "Statistiques complètes", 
               description = "Retourne les statistiques complètes de synchronisation bidirectionnelle")
    public ResponseEntity<Map<String, Object>> getFullStats() {
        log.info("📊 Requête des statistiques complètes de synchronisation");
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Statistiques Firebase (pull)
            long totalFirebase = signalementFirebaseRepository.count();
            stats.put("totalFirebase", totalFirebase);
            
            // Comptage par statut Firebase
            Map<String, Long> firebaseParStatut = new HashMap<>();
            List<Object[]> statsByStatus = signalementFirebaseRepository.countByStatusGrouped();
            for (Object[] row : statsByStatus) {
                String status = (String) row[0];
                Long count = (Long) row[1];
                if (status != null) {
                    firebaseParStatut.put(status, count);
                }
            }
            stats.put("firebaseParStatut", firebaseParStatut);
            
            // Statistiques pour le push
            List<SignalementPushDTO> toPush = syncService.previewSignalementsForPush();
            stats.put("totalAPusher", toPush.size());
            
            long locaux = toPush.stream().filter(s -> "local".equals(s.getSource())).count();
            long firebase = toPush.stream().filter(s -> "firebase".equals(s.getSource())).count();
            stats.put("locauxAPusher", locaux);
            stats.put("firebaseAPusher", firebase);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des stats complètes: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
