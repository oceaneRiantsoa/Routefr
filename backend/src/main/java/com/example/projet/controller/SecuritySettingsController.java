package com.example.projet.controller;

import com.example.projet.dto.SecuritySettingsDTO;
import com.example.projet.service.SecuritySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour gérer les paramètres de sécurité
 */
@RestController
@RequestMapping("/api/manager/security-settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Settings", description = "Gestion des paramètres de sécurité")
public class SecuritySettingsController {
    
    private final SecuritySettingsService securitySettingsService;
    
    /**
     * Récupère les paramètres de sécurité actuels
     */
    @GetMapping
    @Operation(summary = "Récupérer les paramètres de sécurité")
    public ResponseEntity<SecuritySettingsDTO> getSettings() {
        log.info("Récupération des paramètres de sécurité");
        SecuritySettingsDTO settings = securitySettingsService.getSettings();
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Met à jour les paramètres de sécurité
     */
    @PutMapping
    @Operation(summary = "Mettre à jour les paramètres de sécurité")
    public ResponseEntity<SecuritySettingsDTO> updateSettings(@RequestBody SecuritySettingsDTO dto) {
        log.info("Mise à jour des paramètres de sécurité: {}", dto);
        SecuritySettingsDTO updated = securitySettingsService.updateSettings(dto);
        return ResponseEntity.ok(updated);
    }
}
