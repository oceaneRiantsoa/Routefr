package com.example.projet.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = null;
                
                // Essayer depuis le classpath (développement)
                try {
                    log.info("📂 Tentative: chargement depuis classpath...");
                    serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
                    log.info("✅ Fichier trouvé dans classpath");
                } catch (FileNotFoundException e) {
                    // Essayer depuis le volume Docker
                    log.warn("Fichier non trouvé dans classpath, tentative depuis volume Docker...");
                    Resource resource = new FileSystemResource("/app/serviceAccountKey.json");
                    if (resource.exists()) {
                        serviceAccount = resource.getInputStream();
                        log.info("✅ Fichier trouvé dans volume Docker");
                    } else {
                        throw new FileNotFoundException("serviceAccountKey.json introuvable!");
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                
                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("✅ Firebase App initialisé avec succès!");
                return app;
                
            } catch (IOException e) {
                log.error("❌ ERREUR Firebase: {}", e.getMessage());
                throw e;
            }
        }
        return FirebaseApp.getInstance();
    }
    
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        log.info("🔥 Initialisation Firestore...");
        Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
        log.info("✅ Firestore initialisé avec succès!");
        return firestore;
    }
    
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        log.info("✅ FirebaseAuth bean créé");
        return FirebaseAuth.getInstance(firebaseApp);
    }
}