package com.example.projet.service;

import com.google.firebase.auth.*;
import com.example.projet.dto.*;
import com.example.projet.entity.LocalUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.ConnectException;
import java.net.UnknownHostException;

@Service
@Slf4j
public class FirebaseAuthService {
    
    @Autowired
    private FirebaseAuth firebaseAuth;
    
    @Autowired
    private UserManagementService userManagementService;
    
    @Value("${firebase.api.key}")
    private String firebaseApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    // INSCRIPTION
    public UserResponse register(AuthRequest request) throws FirebaseAuthException {
        log.info("Inscription pour: {}", request.getEmail());
        
        // 1. Créer utilisateur dans Firebase
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
            .setEmail(request.getEmail())
            .setPassword(request.getPassword())
            .setDisplayName(request.getDisplayName())
            .setDisabled(false);
        
        UserRecord userRecord = firebaseAuth.createUser(createRequest);
        log.info("Utilisateur créé dans Firebase: {}", userRecord.getUid());
        
        // 2. Ajouter des claims personnalisés (rôle)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER"); // Par défaut
        
        firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);
        
        // 3. Créer enregistrement local pour règles métier + stocker le hash du mot de passe
        userManagementService.createLocalUserWithPassword(
            userRecord.getUid(),
            request.getEmail(),
            request.getDisplayName(),
            passwordEncoder.encode(request.getPassword())
        );
        
        // 4. Générer token personnalisé pour client
        String customToken = firebaseAuth.createCustomToken(userRecord.getUid());
        
        return UserResponse.builder()
            .uid(userRecord.getUid())
            .email(userRecord.getEmail())
            .displayName(userRecord.getDisplayName())
            .role("USER")
            .emailVerified(userRecord.isEmailVerified())
            .accountLocked(false)
            .failedAttempts(0)
            .createdAt(Instant.ofEpochMilli(userRecord.getUserMetadata().getCreationTimestamp()).toString())
            .build();
    }
    
    // CONNEXION - Toujours essayer Firebase d'abord
    public String login(LoginRequest request) throws FirebaseAuthException {
        log.info("Tentative de connexion pour: {}", request.getEmail());
        
        // 1. Vérifier règles métier (limite tentatives)
        userManagementService.checkLoginAttempts(request.getEmail());
        
        // 2. Toujours essayer Firebase d'abord
        try {
            log.info("Tentative de connexion via Firebase...");
            return loginWithFirebase(request);
        } catch (Exception e) {
            // 3. Si erreur réseau → fallback vers mode local
            if (isNetworkError(e)) {
                log.warn("Erreur réseau Firebase, tentative mode hors ligne: {}", e.getMessage());
                return loginLocally(request);
            }
            // 4. Sinon, propager l'erreur (mauvais mot de passe, compte désactivé, etc.)
            log.error("Erreur Firebase (non réseau): {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifie si l'exception est due à un problème réseau
     */
    private boolean isNetworkError(Exception e) {
        // Vérifier le type d'exception
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e.getCause() instanceof ConnectException) {
            return true;
        }
        if (e.getCause() instanceof UnknownHostException) {
            return true;
        }
        
        // Vérifier le message d'erreur
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("connection") || 
               message.contains("timeout") || 
               message.contains("network") ||
               message.contains("unreachable") ||
               message.contains("unknown host") ||
               message.contains("no route to host") ||
               message.contains("connect timed out") ||
               message.contains("failed to connect");
    }

    private String loginWithFirebase(LoginRequest request) throws FirebaseAuthException {
        // Vérifier le mot de passe via Firebase REST API
        String idToken = verifyPasswordWithFirebase(request.getEmail(), request.getPassword());
        
        // Réinitialiser les tentatives échouées
        userManagementService.resetFailedAttempts(request.getEmail());
        
        // Récupérer les infos utilisateur depuis Firebase
        UserRecord userRecord = firebaseAuth.getUserByEmail(request.getEmail());
        
        if (userRecord.isDisabled()) {
            throw new RuntimeException("Compte désactivé");
        }
        
        // Synchroniser et stocker le hash du mot de passe pour usage hors ligne
        userManagementService.syncFirebaseUserToLocal(
            userRecord.getUid(),
            userRecord.getEmail(),
            userRecord.getDisplayName(),
            passwordEncoder.encode(request.getPassword())
        );
        
        log.info("Connexion Firebase réussie pour: {}", request.getEmail());
        return firebaseAuth.createCustomToken(userRecord.getUid());
    }

    private String loginLocally(LoginRequest request) {
        log.info("Mode hors ligne activé pour: {}", request.getEmail());
        
        // Chercher l'utilisateur dans la base locale
        LocalUser localUser = userManagementService.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé. Vous devez vous connecter au moins une fois avec Internet."));
        
        // Vérifier si le compte est bloqué
        if (localUser.isAccountLocked()) {
            throw new RuntimeException("Compte bloqué après 3 tentatives échouées");
        }
        
        // Vérifier si le mot de passe hashé existe
        if (localUser.getPasswordHash() == null || localUser.getPasswordHash().isEmpty()) {
            throw new RuntimeException("Ce compte n'a jamais été utilisé en ligne. Connectez-vous d'abord avec Internet.");
        }
        
        // Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getPassword(), localUser.getPasswordHash())) {
            userManagementService.incrementFailedAttempts(request.getEmail());
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        // Succès - Réinitialiser les tentatives et mettre à jour last_login
        userManagementService.resetFailedAttempts(request.getEmail());
        localUser.setLastLogin(LocalDateTime.now());
        userManagementService.save(localUser);
        
        log.info("Connexion locale réussie pour: {}", request.getEmail());
        
        // Retourner un token local (préfixé pour le distinguer d'un token Firebase)
        return "local-token-" + UUID.randomUUID().toString();
    }
    
    /**
     * Vérifier le mot de passe en utilisant Firebase Auth REST API
     */
    private String verifyPasswordWithFirebase(String email, String password) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("idToken")) {
                return (String) responseBody.get("idToken");
            }
            
            throw new RuntimeException("Token non reçu de Firebase");
            
        } catch (HttpClientErrorException e) {
            log.error("Erreur Firebase Auth API: {}", e.getResponseBodyAsString());
            
            // Incrémenter les tentatives échouées
            userManagementService.incrementFailedAttempts(email);
            
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Email ou mot de passe incorrect");
            }
            throw new RuntimeException("Erreur authentification Firebase");
        }
    }
    
    // VERIFICATION TOKEN (pour API calls)
    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(idToken);
    }
    
    // GET USER BY EMAIL
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return firebaseAuth.getUserByEmail(email);
    }
    
    // MODIFICATION INFOS UTILISATEUR
    public UserResponse updateUser(String uid, Map<String, Object> updates) throws FirebaseAuthException {
        UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(uid);
        
        if (updates.containsKey("displayName")) {
            updateRequest.setDisplayName((String) updates.get("displayName"));
        }
        if (updates.containsKey("email")) {
            updateRequest.setEmail((String) updates.get("email"));
        }
        
        UserRecord updatedUser = firebaseAuth.updateUser(updateRequest);
        
        // Mettre à jour local aussi
        userManagementService.updateLocalUser(uid, updates);
        
        return convertToUserResponse(updatedUser);
    }
    
    // DESACTIVER COMPTE
    public void disableUser(String uid) throws FirebaseAuthException {
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
            .setDisabled(true);
        firebaseAuth.updateUser(request);
        
        userManagementService.disableLocalUser(uid);
    }
    
    // REACTIVER COMPTE (Manager)
    public void enableUser(String uid) throws FirebaseAuthException {
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
            .setDisabled(false);
        firebaseAuth.updateUser(request);
        
        userManagementService.enableLocalUser(uid);
    }
    
    private UserResponse convertToUserResponse(UserRecord userRecord) {
        return UserResponse.builder()
            .uid(userRecord.getUid())
            .email(userRecord.getEmail())
            .displayName(userRecord.getDisplayName())
            .emailVerified(userRecord.isEmailVerified())
            .createdAt(Instant.ofEpochMilli(userRecord.getUserMetadata().getCreationTimestamp()).toString())
            .build();
    }
}