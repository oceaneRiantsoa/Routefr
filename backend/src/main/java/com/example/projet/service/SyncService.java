package com.example.projet.service;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
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
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private final Firestore firestore;
    private final SignalementFirebaseRepository signalementFirebaseRepository;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Synchroniser tous les signalements depuis Firebase vers PostgreSQL
     */
    @Transactional
    public SyncResultDTO syncSignalementsFromFirebase() {
        log.info("🔄 Début de la synchronisation des signalements depuis Firebase...");
        
        List<String> erreurs = new ArrayList<>();
        int nouveaux = 0;
        int misAJour = 0;
        int ignores = 0;
        int totalFirebase = 0;
        
        try {
            // Récupérer tous les signalements depuis Firebase
            CollectionReference signalementsRef = firestore.collection("signalements");
            ApiFuture<QuerySnapshot> future = signalementsRef.get();
            QuerySnapshot querySnapshot = future.get();
            
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            totalFirebase = documents.size();
            
            log.info("📥 {} signalements trouvés dans Firebase", totalFirebase);
            
            List<FirebaseSignalementDTO> signalementsSyncros = new ArrayList<>();
            
            for (QueryDocumentSnapshot document : documents) {
                try {
                    FirebaseSignalementDTO dto = mapDocumentToDTO(document);
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
                            misAJour++;
                            log.debug("📝 Signalement mis à jour: {}", dto.getId());
                        } else {
                            ignores++;
                            log.debug("⏭️ Signalement inchangé, ignoré: {}", dto.getId());
                        }
                    } else {
                        // Créer un nouveau signalement
                        SignalementFirebase newEntity = createEntity(dto);
                        signalementFirebaseRepository.save(newEntity);
                        nouveaux++;
                        log.debug("✨ Nouveau signalement créé: {}", dto.getId());
                    }
                    
                } catch (Exception e) {
                    String erreur = "Erreur sur document " + document.getId() + ": " + e.getMessage();
                    erreurs.add(erreur);
                    log.error("❌ {}", erreur);
                }
            }
            
            log.info("✅ Synchronisation terminée - Nouveaux: {}, Mis à jour: {}, Ignorés: {}, Erreurs: {}",
                    nouveaux, misAJour, ignores, erreurs.size());
            
            return SyncResultDTO.builder()
                    .success(true)
                    .message("Synchronisation réussie")
                    .totalFirebase(totalFirebase)
                    .nouveaux(nouveaux)
                    .misAJour(misAJour)
                    .ignores(ignores)
                    .erreurs(erreurs.size())
                    .erreursDetails(erreurs.isEmpty() ? null : erreurs)
                    .dateSynchronisation(LocalDateTime.now())
                    .signalementsSynchronises(signalementsSyncros)
                    .build();
                    
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ Erreur lors de la synchronisation Firebase: {}", e.getMessage());
            Thread.currentThread().interrupt();
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
        log.info("👀 Aperçu des signalements Firebase...");
        
        CollectionReference signalementsRef = firestore.collection("signalements");
        ApiFuture<QuerySnapshot> future = signalementsRef.get();
        QuerySnapshot querySnapshot = future.get();
        
        List<FirebaseSignalementDTO> signalements = new ArrayList<>();
        for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
            try {
                signalements.add(mapDocumentToDTO(document));
            } catch (Exception e) {
                log.warn("Erreur lors du mapping du document {}: {}", document.getId(), e.getMessage());
            }
        }
        
        log.info("📋 {} signalements récupérés depuis Firebase", signalements.size());
        return signalements;
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
     * Mapper un document Firestore vers un DTO
     */
    private FirebaseSignalementDTO mapDocumentToDTO(QueryDocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        
        return FirebaseSignalementDTO.builder()
                .id(document.getId())
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
    
    // Méthodes utilitaires pour extraire les valeurs de Firestore
    
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
        
        // Firebase Timestamp
        if (value instanceof com.google.cloud.Timestamp) {
            return ((com.google.cloud.Timestamp) value).toDate().getTime();
        }
        // Firestore Timestamp 
        if (value instanceof com.google.firebase.database.annotations.Nullable) {
            return null;
        }
        // Déjà un timestamp en millisecondes
        if (value instanceof Number) {
            return ((Number) value).longValue();
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
