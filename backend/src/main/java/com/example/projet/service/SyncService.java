package com.example.projet.service;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.google.firebase.database.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final FirebaseDatabase firebaseDatabase;
    private final SignalementFirebaseRepository signalementFirebaseRepository;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String SIGNALEMENTS_PATH = "signalements";

    /**
     * Synchroniser tous les signalements depuis Firebase Realtime Database vers PostgreSQL
     */
    @Transactional
    public SyncResultDTO syncSignalementsFromFirebase() {
        log.info("🔄 Début de la synchronisation des signalements depuis Firebase Realtime Database...");
        
        List<String> erreurs = new ArrayList<>();
        int[] counts = {0, 0, 0, 0}; // nouveaux, misAJour, ignores, totalFirebase
        
        CompletableFuture<SyncResultDTO> future = new CompletableFuture<>();
        List<FirebaseSignalementDTO> signalementsSyncros = new ArrayList<>();
        
        DatabaseReference signalementsRef = firebaseDatabase.getReference(SIGNALEMENTS_PATH);
        
        signalementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (!dataSnapshot.exists()) {
                        log.warn("⚠️ Aucune donnée trouvée dans le chemin '{}'", SIGNALEMENTS_PATH);
                        future.complete(SyncResultDTO.builder()
                                .success(true)
                                .message("Aucun signalement trouvé dans Firebase")
                                .totalFirebase(0)
                                .nouveaux(0)
                                .misAJour(0)
                                .ignores(0)
                                .erreurs(0)
                                .dateSynchronisation(LocalDateTime.now())
                                .build());
                        return;
                    }
                    
                    counts[3] = (int) dataSnapshot.getChildrenCount();
                    log.info("📥 {} signalements trouvés dans Firebase Realtime Database", counts[3]);
                    
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        try {
                            FirebaseSignalementDTO dto = mapSnapshotToDTO(childSnapshot);
                            signalementsSyncros.add(dto);
                            
                            // Vérifier si le signalement existe déjà
                            Optional<SignalementFirebase> existant = signalementFirebaseRepository
                                    .findByFirebaseId(dto.getId());
                            
                            if (existant.isPresent()) {
                                // Mettre à jour si nécessaire
                                SignalementFirebase entity = existant.get();
                                if (shouldUpdate(entity, dto)) {
                                    updateEntity(entity, dto);
                                    signalementFirebaseRepository.save(entity);
                                    counts[1]++; // misAJour
                                    log.debug("📝 Signalement mis à jour: {}", dto.getId());
                                } else {
                                    counts[2]++; // ignores
                                    log.debug("⏭️ Signalement inchangé, ignoré: {}", dto.getId());
                                }
                            } else {
                                // Créer un nouveau signalement
                                SignalementFirebase newEntity = createEntity(dto);
                                signalementFirebaseRepository.save(newEntity);
                                counts[0]++; // nouveaux
                                log.debug("✨ Nouveau signalement créé: {}", dto.getId());
                            }
                            
                        } catch (Exception e) {
                            String erreur = "Erreur sur document " + childSnapshot.getKey() + ": " + e.getMessage();
                            erreurs.add(erreur);
                            log.error("❌ {}", erreur);
                        }
                    }
                    
                    log.info("✅ Synchronisation terminée - Nouveaux: {}, Mis à jour: {}, Ignorés: {}, Erreurs: {}",
                            counts[0], counts[1], counts[2], erreurs.size());
                    
                    future.complete(SyncResultDTO.builder()
                            .success(true)
                            .message("Synchronisation réussie")
                            .totalFirebase(counts[3])
                            .nouveaux(counts[0])
                            .misAJour(counts[1])
                            .ignores(counts[2])
                            .erreurs(erreurs.size())
                            .erreursDetails(erreurs.isEmpty() ? null : erreurs)
                            .dateSynchronisation(LocalDateTime.now())
                            .signalementsSynchronises(signalementsSyncros)
                            .build());
                            
                } catch (Exception e) {
                    log.error("❌ Erreur lors du traitement des données: {}", e.getMessage());
                    future.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("❌ Erreur Firebase: {}", databaseError.getMessage());
                future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
            }
        });
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("❌ Erreur lors de la synchronisation Firebase: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return SyncResultDTO.builder()
                    .success(false)
                    .message("Erreur de connexion à Firebase: " + e.getMessage())
                    .dateSynchronisation(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Récupérer les signalements depuis Firebase sans les sauvegarder (aperçu)
     */
    public List<FirebaseSignalementDTO> previewSignalementsFromFirebase() throws ExecutionException, InterruptedException {
        log.info("👀 Aperçu des signalements Firebase Realtime Database...");
        
        CompletableFuture<List<FirebaseSignalementDTO>> future = new CompletableFuture<>();
        
        DatabaseReference signalementsRef = firebaseDatabase.getReference(SIGNALEMENTS_PATH);
        
        signalementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<FirebaseSignalementDTO> signalements = new ArrayList<>();
                
                if (dataSnapshot.exists()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        try {
                            signalements.add(mapSnapshotToDTO(childSnapshot));
                        } catch (Exception e) {
                            log.warn("Erreur lors du mapping du document {}: {}", childSnapshot.getKey(), e.getMessage());
                        }
                    }
                }
                
                log.info("📋 {} signalements récupérés depuis Firebase", signalements.size());
                future.complete(signalements);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
            }
        });
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException("Timeout lors de la récupération des données Firebase", e);
        }
    }
    
    /**
     * Obtenir le statut actuel de synchronisation
     */
    public SyncResultDTO getSyncStatus() {
        long totalLocal = signalementFirebaseRepository.count();
        
        return SyncResultDTO.builder()
                .success(true)
                .message("Statut actuel")
                .nouveaux((int) totalLocal)
                .dateSynchronisation(LocalDateTime.now())
                .build();
    }

    /**
     * Mapper un DataSnapshot Realtime Database vers un DTO
     */
    @SuppressWarnings("unchecked")
    private FirebaseSignalementDTO mapSnapshotToDTO(DataSnapshot snapshot) {
        Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
        
        if (data == null) {
            throw new IllegalArgumentException("Données null pour le snapshot: " + snapshot.getKey());
        }
        
        return FirebaseSignalementDTO.builder()
                .id(snapshot.getKey())
                .odId(getStringValue(data, "id"))
                .userId(getStringValue(data, "userId"))
                .userEmail(getStringValue(data, "userEmail"))
                .latitude(getDoubleValue(data, "latitude"))
                .longitude(getDoubleValue(data, "longitude"))
                .problemeId(getStringValue(data, "problemeId"))
                .problemeNom(getStringValue(data, "problemeNom"))
                .description(getStringValue(data, "description"))
                .status(getStringValue(data, "status"))
                .surface(getBigDecimalValue(data, "surface"))
                .budget(getBigDecimalValue(data, "budget"))
                .dateCreation(getTimestampValue(data, "dateCreation"))
                .entrepriseId(getStringValue(data, "entrepriseId"))
                .entrepriseNom(getStringValue(data, "entrepriseNom"))
                .photoUrl(getStringValue(data, "photoUrl"))
                .build();
    }
    
    /**
     * Créer une nouvelle entité à partir du DTO
     */
    private SignalementFirebase createEntity(FirebaseSignalementDTO dto) {
        Point geom = null;
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            geom = geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
        }
        
        return SignalementFirebase.builder()
                .firebaseId(dto.getId())
                .userId(dto.getUserId())
                .userEmail(dto.getUserEmail())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .problemeId(dto.getProblemeId())
                .problemeNom(dto.getProblemeNom())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .surface(dto.getSurface())
                .budget(dto.getBudget())
                .dateCreationFirebase(timestampToLocalDateTime(dto.getDateCreation()))
                .photoUrl(dto.getPhotoUrl())
                .entrepriseId(dto.getEntrepriseId())
                .entrepriseNom(dto.getEntrepriseNom())
                .dateSynchronisation(LocalDateTime.now())
                .statutLocal("non_traite") // Statut initial côté manager
                .geom(geom)
                .build();
    }
    
    /**
     * Mettre à jour une entité existante
     */
    private void updateEntity(SignalementFirebase entity, FirebaseSignalementDTO dto) {
        entity.setUserEmail(dto.getUserEmail());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setProblemeId(dto.getProblemeId());
        entity.setProblemeNom(dto.getProblemeNom());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setSurface(dto.getSurface());
        entity.setBudget(dto.getBudget());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setDateSynchronisation(LocalDateTime.now());
        
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            entity.setGeom(geometryFactory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude())));
        }
    }
    
    /**
     * Déterminer si une mise à jour est nécessaire
     */
    private boolean shouldUpdate(SignalementFirebase entity, FirebaseSignalementDTO dto) {
        // Comparer les champs principaux
        return !safeEquals(entity.getStatus(), dto.getStatus()) ||
               !safeEquals(entity.getDescription(), dto.getDescription()) ||
               !safeEquals(entity.getProblemeNom(), dto.getProblemeNom()) ||
               !safeEquals(entity.getSurface(), dto.getSurface()) ||
               !safeEquals(entity.getBudget(), dto.getBudget());
    }
    
    // Méthodes utilitaires pour extraire les valeurs du Realtime Database
    
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Long getTimestampValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        // Déjà un timestamp en millisecondes
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        // Si c'est une chaîne, essayer de parser
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private LocalDateTime timestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
    
    private boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
