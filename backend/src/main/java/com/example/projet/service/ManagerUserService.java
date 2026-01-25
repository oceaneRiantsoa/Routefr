package com.example.projet.service;

import com.example.projet.dto.BlockedUserDTO;
import com.example.projet.entity.LocalUser;
import com.example.projet.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerUserService {

	private final LocalUserRepository localUserRepository;

	/**
	 * Retourne la liste des utilisateurs dont le compte est verrouillé
	 * (accountLocked = true) sous forme de DTO utilisable côté Manager.
	 */
	@Transactional(readOnly = true)
	public List<BlockedUserDTO> getBlockedUsers() {
		List<LocalUser> lockedUsers = localUserRepository.findByAccountLockedTrue();

		return lockedUsers.stream()
			.map(user -> BlockedUserDTO.builder()
					.id(user.getId())
					.firebaseUid(user.getFirebaseUid())
					.email(user.getEmail())
					.displayName(user.getDisplayName())
					.role(user.getRole())
					.failedAttempts(user.getFailedAttempts())
					.accountLocked(user.isAccountLocked())
					.lastLogin(user.getLastLogin())
					.build())
			.collect(Collectors.toList());
	}

	/**
	 * Débloque un utilisateur en remettant accountLocked à false et
	 * en réinitialisant le compteur de tentatives échouées.
	 */
	@Transactional
	public void unblockUser(Long id) {
		LocalUser user = localUserRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'id " + id));

		user.setAccountLocked(false);
		user.setFailedAttempts(0);
		localUserRepository.save(user);
	}
}
