package com.example.projet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration de sécurité Spring Security
 * Permet l'accès public aux endpoints d'authentification
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Désactiver CSRF pour API REST
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Activer CORS avec la config définie
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // API
                                                                                                             // stateless
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics (authentification)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints publics - Carte visiteurs (sans authentification)
                        .requestMatchers("/api/public/**").permitAll()

                        // Endpoints Manager (pour interface web React)
                        .requestMatchers("/api/sync/**").permitAll()
                        .requestMatchers("/api/signalements/**").permitAll()
                        .requestMatchers("/api/manager/**").permitAll()
                        .requestMatchers("/api/security-settings/**").permitAll()

                        // Swagger UI (documentation)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/api-docs/**",           // ← AJOUTER
                                "/api-docs",              // ← AJOUTER
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/configuration/ui",      // ← AJOUTER
                                "/configuration/security" // ← AJOUTER
                        )
                        .permitAll()

                        // Health check
                        .requestMatchers("/actuator/**", "/health/**").permitAll()

                        // Ressources statiques
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // Page d'erreur
                        .requestMatchers("/error").permitAll()

                        // Tous les autres endpoints nécessitent une authentification
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable()) // Désactiver Basic Auth
                .formLogin(form -> form.disable()); // Désactiver form login

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Autoriser toutes les origines pour le développement
        configuration.setAllowedOriginPatterns(List.of("*"));
        // Autoriser toutes les méthodes HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        // Autoriser tous les headers
        configuration.setAllowedHeaders(List.of("*"));
        // Exposer les headers de réponse
        configuration.setExposedHeaders(List.of("*"));
        // Autoriser les credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        // Durée du cache preflight
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
