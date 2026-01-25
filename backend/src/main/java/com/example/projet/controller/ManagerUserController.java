package com.example.projet.controller;

import com.example.projet.dto.BlockedUserDTO;
import com.example.projet.service.ManagerUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerUserController {

	private final ManagerUserService managerUserService;

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
}
