package com.example.projet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUserDTO {

	private Long id;
	private String firebaseUid;
	private String email;
	private String displayName;
	private String role;
	private int failedAttempts;
	private boolean accountLocked;
	private LocalDateTime lastLogin;
}
