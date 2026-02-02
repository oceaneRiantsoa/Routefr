package com.example.projet.service;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.PushResultDTO;
import com.example.projet.dto.SignalementPushDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.repository.SignalementDetailsRepository;
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
import java.util.HashMap;
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
    private final SignalementDetailsRepository signalementDetailsRepository;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String SIGNALEMENTS_PATH = "signalements";
    private static final String SIGNALEMENTS_MOBILE_PATH = "signalements_mobile";

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
            return future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("❌ Erreur lors de la synchronisation Firebase: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            
            // Message d'erreur plus descriptif
            String errorMsg;
            if (e instanceof TimeoutException) {
                errorMsg = "Timeout - Vérifiez votre connexion Internet et les règles de sécurité Firebase";
            } else if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            } else {
                errorMsg = e.getMessage();
            }
            
            return SyncResultDTO.builder()
                    .success(false)
                    .message("Erreur de connexion à Firebase: " + errorMsg)
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
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException("Timeout lors de la récupération des données Firebase - Vérifiez votre connexion Internet", e);
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
    
    // ============================================================================
    // PUSH VERS FIREBASE - Envoi des données pour l'affichage mobile
    // ============================================================================
    
    /**
     * Tester la connexion et les droits d'écriture Firebase
     */
    private boolean testFirebaseWriteAccess() {
        try {
            log.info("🔍 Test des droits d'écriture Firebase...");
            DatabaseReference testRef = firebaseDatabase.getReference(SIGNALEMENTS_MOBILE_PATH + "/_test_write");
            
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", true);
            testData.put("timestamp", System.currentTimeMillis());
            
            testRef.setValue(testData, (error, ref) -> {
                if (error != null) {
                    log.error("❌ Test écriture Firebase échoué: {}", error.getMessage());
                    future.complete(false);
                } else {
                    // Supprimer le test avec CompletionListener
                    testRef.removeValue((removeError, removeRef) -> {
                        if (removeError != null) {
                            log.warn("⚠️ Nettoyage test échoué (non bloquant): {}", removeError.getMessage());
                        }
                    });
                    log.info("✅ Test écriture Firebase réussi");
                    future.complete(true);
                }
            });
            
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("❌ Timeout lors du test d'écriture Firebase - Les règles Firebase bloquent probablement l'écriture");
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur test écriture Firebase: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoyer tous les signalements (locaux + Firebase synchronisés) vers Firebase
     * pour l'affichage sur l'application mobile
     */
    public PushResultDTO pushAllSignalementsToFirebase() {
        log.info("📤 Début de l'envoi des signalements vers Firebase pour affichage mobile...");
        
        // Tester d'abord les droits d'écriture
        if (!testFirebaseWriteAccess()) {
            return PushResultDTO.builder()
                    .success(false)
                    .message("❌ Impossible d'écrire dans Firebase. Veuillez configurer les règles Firebase Realtime Database pour autoriser l'écriture sur le chemin 'signalements_mobile'. Voir GUIDE_PUSH_FIREBASE.md")
                    .erreurs(1)
                    .erreursDetails(List.of(
                        "Les règles Firebase Realtime Database bloquent l'écriture.",
                        "Allez dans Firebase Console > Realtime Database > Règles",
                        "Ajoutez: \"signalements_mobile\": { \".read\": true, \".write\": true }"
                    ))
                    .datePush(LocalDateTime.now())
                    .build();
        }
        
        List<String> erreurs = new ArrayList<>();
        List<String> envoyes = new ArrayList<>();
        int[] counts = {0, 0, 0}; // nouveaux, misAJour, erreurs
        
        try {
            // 1. Récupérer tous les signalements de la base locale
            List<SignalementPushDTO> allSignalements = getAllSignalementsForPush();
            log.info("📊 {} signalements à envoyer vers Firebase", allSignalements.size());
            
            if (allSignalements.isEmpty()) {
                return PushResultDTO.builder()
                        .success(true)
                        .message("Aucun signalement à envoyer")
                        .totalEnvoyes(0)
                        .datePush(LocalDateTime.now())
                        .build();
            }
            
            // 2. Référence vers le nœud Firebase pour les données mobiles
            DatabaseReference mobileRef = firebaseDatabase.getReference(SIGNALEMENTS_MOBILE_PATH);
            
            // 3. Envoyer chaque signalement
            for (SignalementPushDTO dto : allSignalements) {
                try {
                    // Convertir le DTO en Map pour Firebase
                    Map<String, Object> signalementData = convertToFirebaseMap(dto);
                    
                    // Générer une clé unique si pas d'ID Firebase
                    String firebaseKey = dto.getId() != null ? dto.getId() : "local_" + dto.getLocalId();
                    
                    // Envoyer vers Firebase (synchrone avec CompletableFuture)
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    
                    mobileRef.child(firebaseKey).setValue(signalementData, (error, ref) -> {
                        if (error != null) {
                            future.completeExceptionally(new RuntimeException(error.getMessage()));
                        } else {
                            future.complete(null);
                        }
                    });
                    
                    // Attendre la confirmation (timeout 5 secondes par signalement)
                    future.get(5, TimeUnit.SECONDS);
                    
                    envoyes.add(firebaseKey);
                    if ("local".equals(dto.getSource())) {
                        counts[0]++; // nouveau
                    } else {
                        counts[1]++; // mis à jour
                    }
                    
                    log.debug("✅ Signalement envoyé: {}", firebaseKey);
                    
                } catch (TimeoutException e) {
                    counts[2]++; // erreur
                    String firebaseKey = dto.getId() != null ? dto.getId() : "local_" + dto.getLocalId();
                    String errMsg = "Timeout envoi " + firebaseKey + " - Vérifiez les règles Firebase";
                    erreurs.add(errMsg);
                    log.error("❌ {}", errMsg);
                } catch (Exception e) {
                    counts[2]++; // erreur
                    String firebaseKey = dto.getId() != null ? dto.getId() : "local_" + dto.getLocalId();
                    String errMsg = "Erreur envoi " + firebaseKey + ": " + (e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
                    erreurs.add(errMsg);
                    log.error("❌ {}", errMsg);
                }
            }
            
            // 4. Mettre à jour les métadonnées si au moins un envoi réussi
            if (!envoyes.isEmpty()) {
                updatePushMetadata(mobileRef, envoyes.size());
            }
            
            log.info("✅ Push terminé - Nouveaux: {}, Mis à jour: {}, Erreurs: {}", 
                    counts[0], counts[1], counts[2]);
            
            return PushResultDTO.builder()
                    .success(counts[2] == 0)
                    .message(counts[2] == 0 ? "✅ Envoi réussi vers Firebase" : "Envoi terminé avec des erreurs")
                    .totalEnvoyes(envoyes.size())
                    .nouveaux(counts[0])
                    .misAJour(counts[1])
                    .erreurs(counts[2])
                    .erreursDetails(erreurs.isEmpty() ? null : erreurs)
                    .signalementsEnvoyes(envoyes)
                    .datePush(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erreur globale lors du push Firebase: {}", e.getMessage());
            return PushResultDTO.builder()
                    .success(false)
                    .message("Erreur lors de l'envoi vers Firebase: " + e.getMessage())
                    .datePush(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Envoyer un seul signalement vers Firebase (par son ID local ou Firebase)
     */
    public PushResultDTO pushSingleSignalement(String signalementId) {
        log.info("📤 Envoi du signalement {} vers Firebase...", signalementId);
        
        try {
            SignalementPushDTO dto = null;
            
            // Chercher d'abord dans les signalements Firebase
            if (signalementId.startsWith("local_")) {
                // C'est un ID local
                Long localId = Long.parseLong(signalementId.replace("local_", ""));
                dto = getLocalSignalementForPush(localId);
            } else {
                // C'est un ID Firebase ou un ID local numérique
                try {
                    Long localId = Long.parseLong(signalementId);
                    // Vérifier si c'est un ID avec offset (Firebase)
                    if (localId >= 10000) {
                        dto = getFirebaseSignalementForPush(localId - 10000);
                    } else {
                        dto = getLocalSignalementForPush(localId);
                    }
                } catch (NumberFormatException e) {
                    // C'est un ID Firebase string
                    dto = getFirebaseSignalementByFirebaseId(signalementId);
                }
            }
            
            if (dto == null) {
                return PushResultDTO.builder()
                        .success(false)
                        .message("Signalement non trouvé: " + signalementId)
                        .datePush(LocalDateTime.now())
                        .build();
            }
            
            // Envoyer vers Firebase
            DatabaseReference mobileRef = firebaseDatabase.getReference(SIGNALEMENTS_MOBILE_PATH);
            String firebaseKey = dto.getId() != null ? dto.getId() : "local_" + dto.getLocalId();
            
            Map<String, Object> signalementData = convertToFirebaseMap(dto);
            
            CompletableFuture<Void> future = new CompletableFuture<>();
            mobileRef.child(firebaseKey).setValue(signalementData, (error, ref) -> {
                if (error != null) {
                    future.completeExceptionally(new RuntimeException(error.getMessage()));
                } else {
                    future.complete(null);
                }
            });
            
            future.get(30, TimeUnit.SECONDS);
            
            log.info("✅ Signalement {} envoyé avec succès", firebaseKey);
            
            return PushResultDTO.builder()
                    .success(true)
                    .message("Signalement envoyé avec succès")
                    .totalEnvoyes(1)
                    .nouveaux(dto.getSource().equals("local") ? 1 : 0)
                    .misAJour(dto.getSource().equals("firebase") ? 1 : 0)
                    .signalementsEnvoyes(List.of(firebaseKey))
                    .datePush(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi du signalement {}: {}", signalementId, e.getMessage());
            return PushResultDTO.builder()
                    .success(false)
                    .message("Erreur lors de l'envoi: " + e.getMessage())
                    .datePush(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Aperçu des signalements qui seront envoyés vers Firebase
     */
    public List<SignalementPushDTO> previewSignalementsForPush() {
        return getAllSignalementsForPush();
    }
    
    /**
     * Récupérer tous les signalements formatés pour l'envoi Firebase
     */
    private List<SignalementPushDTO> getAllSignalementsForPush() {
        List<SignalementPushDTO> result = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        
        // 1. Signalements locaux (signalement_details)
        List<Object[]> localResults = signalementDetailsRepository.findAllSignalementsForManager();
        for (Object[] row : localResults) {
            result.add(mapLocalToSignalementPushDTO(row, timestamp));
        }
        
        // 2. Signalements Firebase synchronisés (signalement_firebase)
        List<SignalementFirebase> firebaseResults = signalementFirebaseRepository.findAll();
        for (SignalementFirebase entity : firebaseResults) {
            result.add(mapFirebaseToSignalementPushDTO(entity, timestamp));
        }
        
        log.debug("📋 Préparation de {} signalements pour push ({} locaux + {} Firebase)",
                result.size(), localResults.size(), firebaseResults.size());
        
        return result;
    }
    
    /**
     * Récupérer un signalement local formaté pour push
     */
    private SignalementPushDTO getLocalSignalementForPush(Long localId) {
        List<Object[]> results = signalementDetailsRepository.findAllSignalementsForManager();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            if (id.equals(localId)) {
                return mapLocalToSignalementPushDTO(row, System.currentTimeMillis());
            }
        }
        return null;
    }
    
    /**
     * Récupérer un signalement Firebase formaté pour push
     */
    private SignalementPushDTO getFirebaseSignalementForPush(Long firebaseLocalId) {
        return signalementFirebaseRepository.findById(firebaseLocalId)
                .map(entity -> mapFirebaseToSignalementPushDTO(entity, System.currentTimeMillis()))
                .orElse(null);
    }
    
    /**
     * Récupérer un signalement Firebase par son ID Firebase
     */
    private SignalementPushDTO getFirebaseSignalementByFirebaseId(String firebaseId) {
        return signalementFirebaseRepository.findByFirebaseId(firebaseId)
                .map(entity -> mapFirebaseToSignalementPushDTO(entity, System.currentTimeMillis()))
                .orElse(null);
    }
    
    /**
     * Mapper un signalement local vers SignalementPushDTO
     */
    private SignalementPushDTO mapLocalToSignalementPushDTO(Object[] row, long timestamp) {
        Long id = ((Number) row[0]).longValue();
        Integer idStatut = row[14] != null ? ((Number) row[14]).intValue() : 10;
        
        BigDecimal surface = row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO;
        BigDecimal coutParM2 = row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;
        
        String status = getStatusCode(idStatut);
        
        return SignalementPushDTO.builder()
                .id(null) // Pas d'ID Firebase pour les signalements locaux
                .localId(id)
                .latitude(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                .longitude(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                .problemeId(row[4] != null ? row[4].toString() : null)
                .problemeNom(row[4] != null ? row[4].toString() : null)
                .description(row[10] != null ? row[10].toString() : null)
                .status(status)
                .statutLibelle(getStatutLibelle(idStatut))
                .surface(surface)
                .budget(surface.multiply(coutParM2))
                .budgetEstime(row[11] != null ? new BigDecimal(row[11].toString()) : null)
                .coutParM2(coutParM2)
                .entrepriseId(row[8] != null ? row[8].toString() : null)
                .entrepriseNom(row[9] != null ? row[9].toString() : null)
                .notesManager(row[12] != null ? row[12].toString() : null)
                .commentaires(row[10] != null ? row[10].toString() : null)
                .dateCreation(row[5] != null ? ((java.sql.Timestamp) row[5]).getTime() : null)
                .dateModification(row[13] != null ? ((java.sql.Timestamp) row[13]).getTime() : null)
                .datePush(timestamp)
                .userId(null)
                .userEmail(null)
                .photoUrl(null)
                .source("local")
                .couleur(getStatusColor(idStatut))
                .icone(getProblemeIcone(row[4] != null ? row[4].toString() : null))
                .build();
    }
    
    /**
     * Mapper un signalement Firebase vers SignalementPushDTO
     */
    private SignalementPushDTO mapFirebaseToSignalementPushDTO(SignalementFirebase entity, long timestamp) {
        String status = entity.getStatutLocal() != null ? entity.getStatutLocal() : entity.getStatus();
        Integer idStatut = getIdStatutFromCode(status);
        
        return SignalementPushDTO.builder()
                .id(entity.getFirebaseId())
                .localId(entity.getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .problemeId(entity.getProblemeId())
                .problemeNom(entity.getProblemeNom())
                .description(entity.getDescription())
                .status(status)
                .statutLibelle(getStatutLibelle(idStatut))
                .surface(entity.getSurface())
                .budget(entity.getBudget())
                .budgetEstime(entity.getBudgetEstime())
                .coutParM2(BigDecimal.valueOf(28750)) // Valeur par défaut
                .entrepriseId(entity.getEntrepriseId())
                .entrepriseNom(entity.getEntrepriseNom())
                .notesManager(entity.getNotesManager())
                .commentaires(entity.getDescription())
                .dateCreation(entity.getDateCreationFirebase() != null 
                        ? entity.getDateCreationFirebase().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() 
                        : null)
                .dateModification(entity.getDateModificationLocal() != null 
                        ? entity.getDateModificationLocal().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() 
                        : null)
                .datePush(timestamp)
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .photoUrl(entity.getPhotoUrl())
                .source("firebase")
                .couleur(getStatusColor(idStatut))
                .icone(getProblemeIcone(entity.getProblemeNom()))
                .build();
    }
    
    /**
     * Convertir un SignalementPushDTO en Map pour Firebase
     */
    private Map<String, Object> convertToFirebaseMap(SignalementPushDTO dto) {
        Map<String, Object> map = new HashMap<>();
        
        // Identifiants
        if (dto.getId() != null) map.put("id", dto.getId());
        if (dto.getLocalId() != null) map.put("localId", dto.getLocalId());
        
        // Position
        if (dto.getLatitude() != null) map.put("latitude", dto.getLatitude());
        if (dto.getLongitude() != null) map.put("longitude", dto.getLongitude());
        
        // Informations du problème
        if (dto.getProblemeId() != null) map.put("problemeId", dto.getProblemeId());
        if (dto.getProblemeNom() != null) map.put("problemeNom", dto.getProblemeNom());
        if (dto.getDescription() != null) map.put("description", dto.getDescription());
        
        // Statut
        if (dto.getStatus() != null) map.put("status", dto.getStatus());
        if (dto.getStatutLibelle() != null) map.put("statutLibelle", dto.getStatutLibelle());
        
        // Données financières
        if (dto.getSurface() != null) map.put("surface", dto.getSurface().doubleValue());
        if (dto.getBudget() != null) map.put("budget", dto.getBudget().doubleValue());
        if (dto.getBudgetEstime() != null) map.put("budgetEstime", dto.getBudgetEstime().doubleValue());
        if (dto.getCoutParM2() != null) map.put("coutParM2", dto.getCoutParM2().doubleValue());
        
        // Entreprise
        if (dto.getEntrepriseId() != null) map.put("entrepriseId", dto.getEntrepriseId());
        if (dto.getEntrepriseNom() != null) map.put("entrepriseNom", dto.getEntrepriseNom());
        
        // Notes
        if (dto.getNotesManager() != null) map.put("notesManager", dto.getNotesManager());
        if (dto.getCommentaires() != null) map.put("commentaires", dto.getCommentaires());
        
        // Dates
        if (dto.getDateCreation() != null) map.put("dateCreation", dto.getDateCreation());
        if (dto.getDateModification() != null) map.put("dateModification", dto.getDateModification());
        if (dto.getDatePush() != null) map.put("datePush", dto.getDatePush());
        
        // Utilisateur
        if (dto.getUserId() != null) map.put("userId", dto.getUserId());
        if (dto.getUserEmail() != null) map.put("userEmail", dto.getUserEmail());
        
        // Photo et affichage
        if (dto.getPhotoUrl() != null) map.put("photoUrl", dto.getPhotoUrl());
        if (dto.getSource() != null) map.put("source", dto.getSource());
        if (dto.getCouleur() != null) map.put("couleur", dto.getCouleur());
        if (dto.getIcone() != null) map.put("icone", dto.getIcone());
        
        return map;
    }
    
    /**
     * Mettre à jour les métadonnées du push
     */
    private void updatePushMetadata(DatabaseReference mobileRef, int totalCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastPush", System.currentTimeMillis());
        metadata.put("totalSignalements", totalCount);
        metadata.put("source", "manager-web");
        
        mobileRef.child("_metadata").setValue(metadata, (error, ref) -> {
            if (error != null) {
                log.warn("⚠️ Erreur mise à jour métadonnées: {}", error.getMessage());
            }
        });
    }
    
    // Méthodes utilitaires pour le mapping
    
    private String getStatusCode(Integer idStatut) {
        if (idStatut == null) return "nouveau";
        switch (idStatut) {
            case 20: return "en_cours";
            case 30: return "traite";
            case 40: return "rejete";
            default: return "nouveau";
        }
    }
    
    private Integer getIdStatutFromCode(String status) {
        if (status == null) return 10;
        switch (status.toLowerCase()) {
            case "en_cours":
            case "en cours":
                return 20;
            case "traite":
            case "traité":
                return 30;
            case "rejete":
            case "rejeté":
                return 40;
            default:
                return 10;
        }
    }
    
    private String getStatutLibelle(Integer idStatut) {
        if (idStatut == null) return "En attente";
        switch (idStatut) {
            case 20: return "En cours";
            case 30: return "Traité";
            case 40: return "Rejeté";
            default: return "En attente";
        }
    }
    
    private String getStatusColor(Integer idStatut) {
        if (idStatut == null) return "#FFC107"; // Jaune - En attente
        switch (idStatut) {
            case 20: return "#2196F3"; // Bleu - En cours
            case 30: return "#4CAF50"; // Vert - Traité
            case 40: return "#F44336"; // Rouge - Rejeté
            default: return "#FFC107"; // Jaune - En attente
        }
    }
    
    private String getProblemeIcone(String probleme) {
        if (probleme == null) return "warning";
        String lower = probleme.toLowerCase();
        if (lower.contains("nid") || lower.contains("poule") || lower.contains("trou")) {
            return "pothole";
        } else if (lower.contains("fissure") || lower.contains("crack")) {
            return "crack";
        } else if (lower.contains("affaissement") || lower.contains("collapse")) {
            return "collapse";
        } else if (lower.contains("inondation") || lower.contains("flood")) {
            return "flood";
        } else if (lower.contains("debris") || lower.contains("obstacle")) {
            return "debris";
        }
        return "warning";
    }
}

