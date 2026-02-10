package com.example.projet.service;

import com.example.projet.dto.FirebaseSignalementDTO;
import com.example.projet.dto.PushResultDTO;
import com.example.projet.dto.SignalementPushDTO;
import com.example.projet.dto.SyncResultDTO;
import com.example.projet.dto.UserSyncResultDTO;
import com.example.projet.entity.LocalUser;
import com.example.projet.entity.SignalementFirebase;
import com.example.projet.repository.SignalementFirebaseRepository;
import com.example.projet.repository.SignalementDetailsRepository;
import com.example.projet.repository.LocalUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.Duration;
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
    private final LocalUserRepository localUserRepository;
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String SIGNALEMENTS_PATH = "signalements";
    private static final String SIGNALEMENTS_MOBILE_PATH = "signalements_mobile";
    private static final String FIREBASE_DB_URL = "https://test-8f6f5-default-rtdb.firebaseio.com";
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Synchroniser tous les signalements depuis Firebase Realtime Database vers PostgreSQL (via REST API)
     */
    @Transactional
    public SyncResultDTO syncSignalementsFromFirebase() {
        log.info("🔄 Début de la synchronisation des signalements depuis Firebase Realtime Database...");
        
        List<String> erreurs = new ArrayList<>();
        int[] counts = {0, 0, 0, 0}; // nouveaux, misAJour, ignores, totalFirebase
        List<FirebaseSignalementDTO> signalementsSyncros = new ArrayList<>();
        
        try {
            // Récupérer les données via REST API depuis signalements_mobile (où le mobile écrit)
            String url = FIREBASE_DB_URL + "/" + SIGNALEMENTS_MOBILE_PATH + ".json";
            log.info("📡 Requête REST vers: {}", url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("❌ Erreur HTTP: {}", response.statusCode());
                return SyncResultDTO.builder()
                        .success(false)
                        .message("Erreur HTTP: " + response.statusCode())
                        .dateSynchronisation(LocalDateTime.now())
                        .build();
            }
            
            String jsonResponse = response.body();
            
            if (jsonResponse == null || jsonResponse.equals("null") || jsonResponse.isEmpty()) {
                log.warn("⚠️ Aucune donnée trouvée dans le chemin '{}'", SIGNALEMENTS_MOBILE_PATH);
                return SyncResultDTO.builder()
                        .success(true)
                        .message("Aucun signalement trouvé dans Firebase")
                        .totalFirebase(0)
                        .nouveaux(0)
                        .misAJour(0)
                        .ignores(0)
                        .erreurs(0)
                        .dateSynchronisation(LocalDateTime.now())
                        .build();
            }
            
            // Parser le JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.readValue(jsonResponse, Map.class);
            
            counts[3] = dataMap.size();
            log.info("📥 {} signalements trouvés dans Firebase Realtime Database", counts[3]);
            
            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                try {
                    String firebaseId = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> signalementData = (Map<String, Object>) entry.getValue();
                    
                    FirebaseSignalementDTO dto = mapToDTO(firebaseId, signalementData);
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
                    String erreur = "Erreur sur document " + entry.getKey() + ": " + e.getMessage();
                    erreurs.add(erreur);
                    log.error("❌ {}", erreur);
                }
            }
            
            log.info("✅ Synchronisation terminée - Nouveaux: {}, Mis à jour: {}, Ignorés: {}, Erreurs: {}",
                    counts[0], counts[1], counts[2], erreurs.size());
            
            return SyncResultDTO.builder()
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
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation Firebase: {}", e.getMessage());
            return SyncResultDTO.builder()
                    .success(false)
                    .message("Erreur de connexion à Firebase: " + e.getMessage())
                    .dateSynchronisation(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Mapper les données JSON en DTO
     */
    @SuppressWarnings("unchecked")
    private FirebaseSignalementDTO mapToDTO(String firebaseId, Map<String, Object> data) {
        FirebaseSignalementDTO dto = new FirebaseSignalementDTO();
        dto.setId(firebaseId);
        
        if (data.get("latitude") != null) {
            dto.setLatitude(((Number) data.get("latitude")).doubleValue());
        }
        if (data.get("longitude") != null) {
            dto.setLongitude(((Number) data.get("longitude")).doubleValue());
        }
        dto.setProblemeId((String) data.get("problemeId"));
        dto.setProblemeNom((String) data.get("problemeNom"));
        dto.setDescription((String) data.get("description"));
        dto.setStatus((String) data.get("status"));
        if (data.get("surface") != null) {
            dto.setSurface(BigDecimal.valueOf(((Number) data.get("surface")).doubleValue()));
        }
        if (data.get("budget") != null) {
            dto.setBudget(BigDecimal.valueOf(((Number) data.get("budget")).doubleValue()));
        }
        dto.setPhotoUrl((String) data.get("photoUrl"));
        dto.setUserId((String) data.get("userId"));
        dto.setUserEmail((String) data.get("userEmail"));
        if (data.get("dateCreation") != null) {
            dto.setDateCreation(((Number) data.get("dateCreation")).longValue());
        }
        dto.setEntrepriseId((String) data.get("entrepriseId"));
        dto.setEntrepriseNom((String) data.get("entrepriseNom"));
        
        // Récupérer les photos base64 (nouveau système)
        Object photosObj = data.get("photos");
        if (photosObj instanceof List) {
            dto.setPhotos((List<String>) photosObj);
        }
        
        return dto;
    }
    
    /**
     * Récupérer les signalements depuis Firebase sans les sauvegarder (aperçu) - via REST API
     */
    public List<FirebaseSignalementDTO> previewSignalementsFromFirebase() throws ExecutionException, InterruptedException {
        log.info("👀 Aperçu des signalements Firebase Realtime Database (REST API)...");
        
        try {
            String url = FIREBASE_DB_URL + "/" + SIGNALEMENTS_MOBILE_PATH + ".json";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new ExecutionException("Erreur HTTP: " + response.statusCode(), null);
            }
            
            String jsonResponse = response.body();
            List<FirebaseSignalementDTO> signalements = new ArrayList<>();
            
            if (jsonResponse != null && !jsonResponse.equals("null") && !jsonResponse.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = objectMapper.readValue(jsonResponse, Map.class);
                
                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> signalementData = (Map<String, Object>) entry.getValue();
                        signalements.add(mapToDTO(entry.getKey(), signalementData));
                    } catch (Exception e) {
                        log.warn("Erreur lors du mapping du document {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
            
            log.info("📋 {} signalements récupérés depuis Firebase", signalements.size());
            return signalements;
            
        } catch (Exception e) {
            throw new ExecutionException("Erreur lors de la récupération des données Firebase: " + e.getMessage(), e);
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
        
        // Convertir les photos en JSON string
        String photosJson = null;
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            try {
                photosJson = objectMapper.writeValueAsString(dto.getPhotos());
            } catch (Exception e) {
                log.warn("Erreur conversion photos en JSON: {}", e.getMessage());
            }
        }
        
        // Mapper le status Firebase vers statutLocal et avancementPourcentage
        String statutLocal = mapFirebaseStatusToLocal(dto.getStatus());
        Integer avancementPourcentage = mapStatusToAvancement(dto.getStatus());

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
                .photos(photosJson)
                .entrepriseId(dto.getEntrepriseId())
                .entrepriseNom(dto.getEntrepriseNom())
                .dateSynchronisation(LocalDateTime.now())
                .statutLocal(statutLocal)
                .avancementPourcentage(avancementPourcentage)
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
        
        // Synchroniser statutLocal et avancementPourcentage depuis le status Firebase
        entity.setStatutLocal(mapFirebaseStatusToLocal(dto.getStatus()));
        entity.setAvancementPourcentage(mapStatusToAvancement(dto.getStatus()));
        
        // Mettre à jour les photos
        if (dto.getPhotos() != null && !dto.getPhotos().isEmpty()) {
            try {
                entity.setPhotos(objectMapper.writeValueAsString(dto.getPhotos()));
            } catch (Exception e) {
                log.warn("Erreur conversion photos en JSON: {}", e.getMessage());
            }
        }
        
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
    // PUSH VERS FIREBASE - Envoi des données pour l'affichage mobile (via REST API)
    // ============================================================================
    
    /**
     * Tester la connexion et les droits d'écriture Firebase via REST API
     */
    private boolean testFirebaseWriteAccess() {
        try {
            log.info("🔍 Test des droits d'écriture Firebase (REST API) sur chemin: {}", SIGNALEMENTS_MOBILE_PATH);
            
            String testUrl = FIREBASE_DB_URL + "/" + SIGNALEMENTS_MOBILE_PATH + "/_test_write.json";
            log.info("📍 URL de test: {}", testUrl);
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", true);
            testData.put("timestamp", System.currentTimeMillis());
            
            String jsonData = objectMapper.writeValueAsString(testData);
            log.info("📤 Envoi des données de test via REST...");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.info("✅ Test écriture Firebase réussi (status: {})", response.statusCode());
                
                // Nettoyer le test
                HttpRequest deleteRequest = HttpRequest.newBuilder()
                        .uri(URI.create(testUrl))
                        .timeout(Duration.ofSeconds(5))
                        .DELETE()
                        .build();
                httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
                log.info("🧹 Nettoyage test réussi");
                
                return true;
            } else {
                log.error("❌ Test écriture Firebase échoué - Status: {}, Body: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Erreur test écriture Firebase: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Envoyer un signalement vers Firebase via REST API
     */
    private boolean pushSignalementViaRest(String key, SignalementPushDTO signalement) {
        try {
            String url = FIREBASE_DB_URL + "/" + SIGNALEMENTS_MOBILE_PATH + "/" + key + ".json";
            String jsonData = objectMapper.writeValueAsString(signalement);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                log.debug("✅ Signalement {} envoyé avec succès", key);
                return true;
            } else {
                log.error("❌ Erreur envoi signalement {} - Status: {}", key, response.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Erreur envoi signalement {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Envoyer tous les signalements (locaux + Firebase synchronisés) vers Firebase
     * pour l'affichage sur l'application mobile (via REST API)
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
            
            // 2. Envoyer chaque signalement via REST API
            for (SignalementPushDTO dto : allSignalements) {
                try {
                    // Générer une clé unique si pas d'ID Firebase
                    String firebaseKey = dto.getId() != null ? dto.getId() : "local_" + dto.getLocalId();
                    
                    // Envoyer vers Firebase via REST
                    boolean success = pushSignalementViaRest(firebaseKey, dto);
                    
                    if (success) {
                        envoyes.add(firebaseKey);
                        if ("local".equals(dto.getSource())) {
                            counts[0]++; // nouveau
                        } else {
                            counts[1]++; // mis à jour
                        }
                        log.debug("✅ Signalement envoyé: {}", firebaseKey);
                    } else {
                        counts[2]++; // erreur
                        String errMsg = "Erreur envoi " + firebaseKey;
                        erreurs.add(errMsg);
                        log.error("❌ {}", errMsg);
                    }
                    
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
                updatePushMetadataViaRest(envoyes.size());
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
        String rawStatus = entity.getStatutLocal() != null ? entity.getStatutLocal() : entity.getStatus();
        Integer idStatut = getIdStatutFromCode(rawStatus);
        String normalizedStatus = getStatusCode(idStatut); // Toujours: nouveau, en_cours, ou termine
        
        return SignalementPushDTO.builder()
                .id(entity.getFirebaseId())
                .localId(entity.getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .problemeId(entity.getProblemeId())
                .problemeNom(entity.getProblemeNom())
                .description(entity.getDescription())
                .status(normalizedStatus)
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
     * Mettre à jour les métadonnées du push via REST API
     */
    private void updatePushMetadataViaRest(int totalCount) {
        try {
            String url = FIREBASE_DB_URL + "/" + SIGNALEMENTS_MOBILE_PATH + "/_metadata.json";
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("lastPush", System.currentTimeMillis());
            metadata.put("totalSignalements", totalCount);
            metadata.put("source", "manager-web");
            
            String jsonData = objectMapper.writeValueAsString(metadata);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("📊 Métadonnées mises à jour: {} signalements", totalCount);
        } catch (Exception e) {
            log.warn("⚠️ Erreur mise à jour métadonnées: {}", e.getMessage());
        }
    }
    
    // Méthodes utilitaires pour le mapping
    
    private String getStatusCode(Integer idStatut) {
        if (idStatut == null) return "nouveau";
        switch (idStatut) {
            case 20: return "en_cours";
            case 30: return "termine";  // Compatible mobile
            case 40: return "termine";  // Rejeté = terminé pour le mobile
            default: return "nouveau";
        }
    }
    
    private Integer getIdStatutFromCode(String status) {
        if (status == null) return 10;
        switch (status.toLowerCase()) {
            case "nouveau":
            case "non_traite":
                return 10;
            case "en_cours":
            case "en cours":
                return 20;
            case "traite":
            case "traité":
            case "termine":  // Compatible mobile
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
    
    /**
     * Mappe le status Firebase vers un statutLocal normalisé
     */
    private String mapFirebaseStatusToLocal(String firebaseStatus) {
        if (firebaseStatus == null) return "nouveau";
        switch (firebaseStatus.toLowerCase()) {
            case "en_cours":
            case "en cours":
                return "en_cours";
            case "traite":
            case "traité":
            case "termine":
                return "termine";
            case "rejete":
            case "rejeté":
                return "rejete";
            case "nouveau":
            case "non_traite":
            default:
                return "nouveau";
        }
    }
    
    /**
     * Mappe le status Firebase vers un pourcentage d'avancement
     */
    private Integer mapStatusToAvancement(String status) {
        if (status == null) return 0;
        switch (status.toLowerCase()) {
            case "en_cours":
            case "en cours":
                return 50;
            case "traite":
            case "traité":
            case "termine":
                return 100;
            default:
                return 0;
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

    // ==================== SYNCHRONISATION DES UTILISATEURS ====================

    /**
     * Synchronisation bidirectionnelle des utilisateurs
     * 1. PUSH : Envoyer les utilisateurs locaux non synchronisés vers Firebase
     * 2. PULL : Récupérer les utilisateurs Firebase vers local
     */
    @Transactional
    public UserSyncResultDTO syncUsers() {
        log.info("🔄 Début de la synchronisation des utilisateurs...");
        
        int pushedCount = 0;
        int pulledCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        boolean firebaseConnectionError = false;
        
        // 1. PUSH : Utilisateurs locaux → Firebase
        List<LocalUser> usersToSync = localUserRepository.findBySyncedToFirebaseFalse();
        log.info("📤 {} utilisateur(s) à envoyer vers Firebase", usersToSync.size());
        
        for (LocalUser localUser : usersToSync) {
            // Si on a déjà une erreur de connexion Firebase, arrêter les tentatives
            if (firebaseConnectionError) {
                errors.add("Synchronisation ignorée pour " + localUser.getEmail() + " (erreur de connexion Firebase)");
                errorCount++;
                continue;
            }
            
            try {
                // Vérifier si le mot de passe en clair est disponible
                if (localUser.getPasswordPlainTemp() == null || localUser.getPasswordPlainTemp().isEmpty()) {
                    log.warn("⚠️ Mot de passe en clair non disponible pour {}, impossible de synchroniser vers Firebase", localUser.getEmail());
                    errors.add("Mot de passe manquant pour " + localUser.getEmail());
                    errorCount++;
                    continue;
                }
                
                // Créer l'utilisateur dans Firebase
                // Utiliser email comme displayName par défaut si null
                String displayName = localUser.getDisplayName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = localUser.getEmail().split("@")[0]; // Utiliser la partie avant @ comme nom
                }
                
                UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(localUser.getEmail())
                    .setPassword(localUser.getPasswordPlainTemp())
                    .setDisplayName(displayName)
                    .setDisabled(localUser.isAccountLocked());
                
                UserRecord userRecord = firebaseAuth.createUser(createRequest);
                log.info("✅ Utilisateur créé dans Firebase: {} -> UID: {}", localUser.getEmail(), userRecord.getUid());
                
                // Ajouter les claims (rôle)
                Map<String, Object> claims = new HashMap<>();
                claims.put("role", localUser.getRole() != null ? localUser.getRole() : "USER");
                firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);
                
                // Mettre à jour l'utilisateur local
                localUser.setFirebaseUid(userRecord.getUid());
                localUser.setSyncedToFirebase(true);
                localUser.setFirebaseSyncDate(LocalDateTime.now());
                localUser.setPasswordPlainTemp(null); // Effacer le mot de passe en clair
                localUserRepository.save(localUser);
                
                pushedCount++;
                
            } catch (FirebaseAuthException e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "";
                
                // Détecter les erreurs de connexion/authentification Firebase
                if (errorMessage.contains("Invalid JWT Signature") || 
                    errorMessage.contains("invalid_grant") ||
                    errorMessage.contains("Error getting access token")) {
                    log.error("❌ Erreur de configuration Firebase (clé invalide ou expirée)");
                    errors.add("⚠️ Clé Firebase invalide ou expirée. Veuillez mettre à jour serviceAccountKey.json");
                    firebaseConnectionError = true;
                    errorCount++;
                    continue;
                }
                
                if (errorMessage.contains("EMAIL_EXISTS")) {
                    // L'utilisateur existe déjà dans Firebase, récupérer son UID
                    try {
                        UserRecord existingUser = firebaseAuth.getUserByEmail(localUser.getEmail());
                        localUser.setFirebaseUid(existingUser.getUid());
                        localUser.setSyncedToFirebase(true);
                        localUser.setFirebaseSyncDate(LocalDateTime.now());
                        localUser.setPasswordPlainTemp(null);
                        localUserRepository.save(localUser);
                        log.info("✅ Utilisateur existant dans Firebase lié: {} -> UID: {}", localUser.getEmail(), existingUser.getUid());
                        pushedCount++;
                    } catch (FirebaseAuthException ex) {
                        log.error("❌ Erreur lors de la récupération de l'utilisateur Firebase: {}", ex.getMessage());
                        errors.add("Erreur " + localUser.getEmail() + ": " + ex.getMessage());
                        errorCount++;
                    }
                } else {
                    log.error("❌ Erreur création Firebase pour {}: {}", localUser.getEmail(), e.getMessage());
                    errors.add("Erreur " + localUser.getEmail() + ": " + e.getMessage());
                    errorCount++;
                }
            }
        }
        
        // 2. PULL : Firebase → Utilisateurs locaux (optionnel - pour nouveaux utilisateurs créés directement dans Firebase)
        // Cette partie peut être implémentée si nécessaire
        
        String finalMessage;
        if (firebaseConnectionError) {
            finalMessage = "⚠️ Synchronisation impossible: Clé Firebase invalide. L'authentification locale fonctionne normalement.";
        } else {
            finalMessage = String.format("Synchronisation terminée: %d envoyés vers Firebase, %d erreurs", pushedCount, errorCount);
        }
        
        log.info("✅ Synchronisation utilisateurs terminée: {} envoyés, {} récupérés, {} erreurs", 
                pushedCount, pulledCount, errorCount);
        
        return UserSyncResultDTO.builder()
                .success(errorCount == 0 && !firebaseConnectionError)
                .message(finalMessage)
                .pushedToFirebase(pushedCount)
                .pulledFromFirebase(pulledCount)
                .errors(errorCount)
                .errorDetails(errors)
                .syncDate(LocalDateTime.now())
                .build();
    }

    /**
     * Récupère le nombre d'utilisateurs non synchronisés
     */
    public long countUsersNotSynced() {
        return localUserRepository.countBySyncedToFirebaseFalse();
    }

    /**
     * Récupère la liste des utilisateurs non synchronisés
     */
    public List<LocalUser> getUsersNotSynced() {
        return localUserRepository.findBySyncedToFirebaseFalse();
    }
}

