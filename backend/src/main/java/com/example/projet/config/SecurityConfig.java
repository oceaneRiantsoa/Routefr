package com.example.projet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité Spring Security
 * Permet l'accès public aux endpoints d'authentification
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Désactiver CSRF pour API REST
                .cors(cors -> cors.disable()) // Configurer CORS si nécessaire
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // API
                                                                                                             // stateless
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics (authentification)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints publics - Carte visiteurs (sans authentification)
                        .requestMatchers("/api/public/**").permitAll()

                        // Swagger UI (documentation)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
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
}
