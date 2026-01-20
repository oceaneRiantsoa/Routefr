package com.example.projet.service;



import com.google.firebase.auth.*;
import com.example.projet.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
        
        // 3. Créer enregistrement local pour règles métier
        userManagementService.createLocalUser(
            userRecord.getUid(),
            request.getEmail(),
            request.getDisplayName()
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
    
    // CONNEXION
    public String login(LoginRequest request) throws FirebaseAuthException {
        log.info("Tentative de connexion pour: {}", request.getEmail());
        
        // 1. Vérifier règles métier (limite tentatives)
        userManagementService.checkLoginAttempts(request.getEmail());
        
        try {
            // 2. Vérifier le mot de passe avec Firebase Auth REST API
            String idToken = verifyPasswordWithFirebase(request.getEmail(), request.getPassword());
            
            // 3. Si succès, réinitialiser tentatives
            userManagementService.resetFailedAttempts(request.getEmail());
            
            // 4. Récupérer utilisateur
            UserRecord userRecord = firebaseAuth.getUserByEmail(request.getEmail());
            
            // 5. Vérifier si compte désactivé
            if (userRecord.isDisabled()) {
                throw new RuntimeException("Compte désactivé");
            }
            
            // 6. Générer custom token (pour client SDK)
            return firebaseAuth.createCustomToken(userRecord.getUid());
            
        } catch (HttpClientErrorException e) {
            // Incrémenter tentatives échouées
            userManagementService.incrementFailedAttempts(request.getEmail());
            
            // Vérifier si le compte vient d'être bloqué
            try {
                userManagementService.checkLoginAttempts(request.getEmail());
            } catch (RuntimeException blockedException) {
                // Le compte est maintenant bloqué
                throw blockedException;
            }
            
            // Si pas encore bloqué, retourner erreur normale
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Email ou mot de passe incorrect");
            }
            throw new RuntimeException("Erreur lors de la connexion: " + e.getMessage());
            
        } catch (Exception e) {
            // Pour toute autre exception
            if (e.getMessage().contains("bloqué")) {
                throw e; // Relancer l'exception de blocage
            }
            
            userManagementService.incrementFailedAttempts(request.getEmail());
            
            // Vérifier si le compte vient d'être bloqué
            try {
                userManagementService.checkLoginAttempts(request.getEmail());
            } catch (RuntimeException blockedException) {
                throw blockedException;
            }
            
            throw new RuntimeException("Erreur connexion: " + e.getMessage());
        }
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