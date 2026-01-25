package com.example.projet.controller;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Vérification de l'état du système")
public class HealthController {
    
    private final FirebaseAuth firebaseAuth;
    
    @GetMapping("/status")
    @Operation(summary = "Vérifier l'état du système", description = "Vérifier que l'application et Firebase sont opérationnels")
    public ResponseEntity<?> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        
        // Vérifier Spring Boot
        status.put("application", "OK");
        status.put("timestamp", System.currentTimeMillis());
        
        // Vérifier Firebase
        try {
            boolean firebaseInitialized = !FirebaseApp.getApps().isEmpty();
            status.put("firebase", firebaseInitialized ? "✅ CONNECTED" : "❌ NOT INITIALIZED");
            
            if (firebaseInitialized) {
                status.put("firebaseApp", FirebaseApp.getInstance().getName());
                status.put("firebaseProjectId", FirebaseApp.getInstance().getOptions().getProjectId());
            }
            
            // Test connexion FirebaseAuth
            if (firebaseAuth != null) {
                status.put("firebaseAuth", "✅ READY");
            } else {
                status.put("firebaseAuth", "❌ NULL");
            }
            
        } catch (Exception e) {
            status.put("firebase", "❌ ERROR: " + e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/firebase")
    @Operation(summary = "Détails Firebase", description = "Obtenir les détails de la configuration Firebase")
    public ResponseEntity<?> checkFirebase() {
        Map<String, Object> firebaseInfo = new HashMap<>();
        
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                firebaseInfo.put("status", "❌ Firebase non initialisé");
                firebaseInfo.put("solution", "Vérifiez que serviceAccountKey.json existe dans src/main/resources/");
                return ResponseEntity.status(500).body(firebaseInfo);
            }
            
            FirebaseApp app = FirebaseApp.getInstance();
            
            firebaseInfo.put("status", "✅ Firebase connecté");
            firebaseInfo.put("appName", app.getName());
            firebaseInfo.put("projectId", app.getOptions().getProjectId());
            firebaseInfo.put("databaseUrl", app.getOptions().getDatabaseUrl());
            firebaseInfo.put("serviceAccountId", app.getOptions().getServiceAccountId());
            
            return ResponseEntity.ok(firebaseInfo);
            
        } catch (Exception e) {
            firebaseInfo.put("status", "❌ Erreur");
            firebaseInfo.put("error", e.getMessage());
            firebaseInfo.put("solution", "Vérifiez la configuration Firebase");
            return ResponseEntity.status(500).body(firebaseInfo);
        }
    }
}
