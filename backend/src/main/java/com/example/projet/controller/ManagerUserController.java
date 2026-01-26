package com.example.projet.controller;

import com.example.projet.dto.BlockedUserDTO;
import com.example.projet.dto.SecuritySettingsDTO;
import com.example.projet.service.ManagerUserService;
import com.example.projet.service.SecuritySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
public class ManagerUserController {

	private final ManagerUserService managerUserService;
	private final SecuritySettingsService securitySettingsService;

	/**
	 * Liste tous les utilisateurs dont le compte est bloqué.
	 * GET /api/manager/blocked-users
	 */
	@GetMapping("/blocked-users")
	public ResponseEntity<List<BlockedUserDTO>> getBlockedUsers() {
		List<BlockedUserDTO> blockedUsers = managerUserService.getBlockedUsers();
		return ResponseEntity.ok(blockedUsers);
	}

	/**
	 * Débloque un utilisateur par son id.
	 * POST /api/manager/users/{id}/unblock
	 */
	@PostMapping("/users/{id}/unblock")
	public ResponseEntity<Void> unblockUser(@PathVariable Long id) {
		managerUserService.unblockUser(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Récupérer les paramètres de sécurité actuels
	 * GET /api/manager/settings/security
	 */
	@GetMapping("/settings/security")
	public ResponseEntity<SecuritySettingsDTO> getSecuritySettings() {
		log.info("📋 Récupération des paramètres de sécurité");
		return ResponseEntity.ok(securitySettingsService.getSettings());
	}

	/**
	 * Mettre à jour les paramètres de sécurité
	 * PUT /api/manager/settings/security
	 */
	@PutMapping("/settings/security")
	public ResponseEntity<?> updateSecuritySettings(@RequestBody SecuritySettingsDTO settings) {
		log.info("🔧 Mise à jour des paramètres de sécurité: session={}min, maxTentatives={}", 
				settings.getSessionDurationMinutes(), settings.getMaxFailedAttempts());
		try {
			SecuritySettingsDTO updated = securitySettingsService.updateSettings(settings);
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			log.warn("⚠️ Paramètres invalides: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Réinitialiser les paramètres de sécurité aux valeurs par défaut
	 * POST /api/manager/settings/security/reset
	 */
	@PostMapping("/settings/security/reset")
	public ResponseEntity<SecuritySettingsDTO> resetSecuritySettings() {
		log.info("🔄 Réinitialisation des paramètres de sécurité");
		return ResponseEntity.ok(securitySettingsService.resetToDefaults());
	}
}
