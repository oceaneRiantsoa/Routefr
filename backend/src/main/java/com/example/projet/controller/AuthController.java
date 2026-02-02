package com.example.projet.controller;


import com.google.firebase.auth.FirebaseAuthException;
import com.example.projet.dto.*;
import com.example.projet.service.FirebaseAuthService;
import com.example.projet.service.SessionService;
import com.example.projet.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Authentification", description = "API d'authentification Firebase avec gestion des sessions")
public class AuthController {
    
    private final FirebaseAuthService firebaseAuthService;
    private final SessionService sessionService;
    private final UserManagementService userManagementService;
    
    // INSCRIPTION
    @PostMapping("/register")
    @Operation(summary = "Inscription utilisateur", description = "Créer un nouveau compte utilisateur avec Firebase")
    @ApiResponse(responseCode = "201", description = "Inscription réussie")
    @ApiResponse(responseCode = "400", description = "Erreur de validation ou email déjà existant")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            UserResponse user = firebaseAuthService.register(request);
            
            // Créer une session
            String sessionToken = sessionService.createSession(
                user.getUid(),
                getClientIP(httpRequest),
                httpRequest.getHeader("User-Agent")
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("user", user);
            response.put("sessionToken", sessionToken);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    
        } catch (FirebaseAuthException e) {
            log.error("Erreur inscription: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur inscription: " + e.getMessage()));
        }
    }
    
    // CONNEXION
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Authentifier un utilisateur et créer une session")
    @ApiResponse(responseCode = "200", description = "Connexion réussie")
    @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte bloqué")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String customToken = firebaseAuthService.login(request);
            
            // Vérifier si c'est un token local (mode hors ligne)
            boolean isOfflineMode = customToken.startsWith("local-token-");
            
            String uid;
            String email = request.getEmail();
            String sessionToken;
            
            if (isOfflineMode) {
                // Mode hors ligne : récupérer l'UID depuis la base locale
                var localUser = userManagementService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur local non trouvé"));
                uid = localUser.getFirebaseUid();
                
                // Créer une session locale
                sessionToken = sessionService.createSession(
                    uid,
                    getClientIP(httpRequest),
                    httpRequest.getHeader("User-Agent")
                );
            } else {
                // Mode en ligne : récupérer l'utilisateur depuis Firebase
                var userRecord = firebaseAuthService.getUserByEmail(email);
                uid = userRecord.getUid();
                email = userRecord.getEmail();
                
                // Créer une session
                sessionToken = sessionService.createSession(
                    uid,
                    getClientIP(httpRequest),
                    httpRequest.getHeader("User-Agent")
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", isOfflineMode ? "Connexion locale réussie" : "Connexion réussie");
            response.put("customToken", customToken);
            response.put("sessionToken", sessionToken);
            response.put("uid", uid);
            response.put("email", email);
            response.put("offlineMode", isOfflineMode);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur connexion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse("Échec connexion: " + e.getMessage()));
        }
    }
    
    // VERIFICATION TOKEN (pour appels API sécurisés)
    @PostMapping("/verify")
    @Operation(summary = "Vérifier token", description = "Vérifier la validité d'un token Firebase")
    @ApiResponse(responseCode = "200", description = "Token valide")
    @ApiResponse(responseCode = "401", description = "Token invalide ou expiré")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(errorResponse("Token manquant ou invalide"));
            }
            
            String idToken = authHeader.substring(7);
            var decodedToken = firebaseAuthService.verifyToken(idToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("uid", decodedToken.getUid());
            response.put("email", decodedToken.getEmail());
            response.put("claims", decodedToken.getClaims());
            
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse("Token invalide: " + e.getMessage()));
        }
    }
    
    // VERIFICATION SESSION
    @PostMapping("/verify-session")
    @Operation(summary = "Vérifier session", description = "Vérifier la validité d'une session utilisateur")
    @ApiResponse(responseCode = "200", description = "Session valide")
    @ApiResponse(responseCode = "401", description = "Session invalide ou expirée")
    public ResponseEntity<?> verifySession(@RequestHeader("Session-Token") String sessionToken) {
        if (sessionService.isSessionValid(sessionToken)) {
            var session = sessionService.getSession(sessionToken);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session valide",
                "session", session.orElse(null)
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse("Session invalide ou expirée"));
    }
    
    // REFRESH SESSION
    @PostMapping("/refresh-session")
    @Operation(summary = "Prolonger session", description = "Prolonger la durée de vie d'une session active")
    @ApiResponse(responseCode = "200", description = "Session prolongée")
    @ApiResponse(responseCode = "400", description = "Session invalide")
    public ResponseEntity<?> refreshSession(@RequestHeader("Session-Token") String sessionToken) {
        if (sessionService.isSessionValid(sessionToken)) {
            sessionService.refreshSession(sessionToken);
            return ResponseEntity.ok(successResponse("Session prolongée"));
        }
        return ResponseEntity.badRequest()
            .body(errorResponse("Session invalide"));
    }
    
    // LOGOUT (DECONNEXION)
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Invalider la session de l'utilisateur")
    @ApiResponse(responseCode = "200", description = "Déconnexion réussie")
    public ResponseEntity<?> logout(@RequestHeader("Session-Token") String sessionToken) {
        sessionService.invalidateSession(sessionToken);
        return ResponseEntity.ok(successResponse("Déconnexion réussie"));
    }
    
    // LOGOUT ALL (déconnexion de tous les appareils)
    @PostMapping("/logout-all")
    @Operation(summary = "Déconnexion globale", description = "Invalider toutes les sessions de l'utilisateur")
    @ApiResponse(responseCode = "200", description = "Toutes les sessions invalidées")
    public ResponseEntity<?> logoutAll(@RequestHeader("Authorization") String authHeader) {
        try {
            String idToken = authHeader.substring(7);
            var decodedToken = firebaseAuthService.verifyToken(idToken);
            sessionService.invalidateAllUserSessions(decodedToken.getUid());
            return ResponseEntity.ok(successResponse("Toutes les sessions invalidées"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur: " + e.getMessage()));
        }
    }
    
    // MODIFIER INFOS UTILISATEUR
    @PutMapping("/users/{uid}")
    @Operation(summary = "Modifier utilisateur", description = "Mettre à jour les informations d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour")
    public ResponseEntity<?> updateUser(
            @PathVariable String uid,
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Vérifier token
            String idToken = authHeader.substring(7);
            firebaseAuthService.verifyToken(idToken);
            
            UserResponse updatedUser = firebaseAuthService.updateUser(uid, updates);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utilisateur mis à jour",
                "user", updatedUser
            ));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur mise à jour: " + e.getMessage()));
        }
    }
    
    // DESACTIVER COMPTE (Manager)
    @PostMapping("/users/{uid}/disable")
    @Operation(summary = "Désactiver compte", description = "Désactiver le compte d'un utilisateur")
    public ResponseEntity<?> disableUser(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            firebaseAuthService.disableUser(uid);
            return ResponseEntity.ok(successResponse("Compte désactivé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur désactivation: " + e.getMessage()));
        }
    }
    
    // REACTIVER COMPTE (Manager)
    @PostMapping("/users/{uid}/enable")
    @Operation(summary = "Réactiver compte", description = "Réactiver le compte d'un utilisateur")
    public ResponseEntity<?> enableUser(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            firebaseAuthService.enableUser(uid);
            return ResponseEntity.ok(successResponse("Compte réactivé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur réactivation: " + e.getMessage()));
        }
    }
    
    // RESET TENTATIVES (Manager - règle métier)
    @PostMapping("/users/{email}/reset-attempts")
    @Operation(summary = "Réinitialiser tentatives", description = "Réinitialiser les tentatives de connexion et débloquer le compte")
    public ResponseEntity<?> resetAttempts(@PathVariable String email) {
        try {
            userManagementService.resetFailedAttempts(email);
            return ResponseEntity.ok(successResponse("Tentatives réinitialisées pour " + email));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(errorResponse("Erreur: " + e.getMessage()));
        }
    }
    
    // Helper methods
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    private Map<String, Object> successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}