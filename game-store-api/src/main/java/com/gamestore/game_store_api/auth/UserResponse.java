package com.gamestore.game_store_api.auth;

import java.time.LocalDateTime;

import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;

public record UserResponse(Long id, String email, String displayName, Role role, LocalDateTime createdAt) {

	static UserResponse from(UserAccount account) {
		return new UserResponse(account.getId(), account.getEmail(), account.getDisplayName(),
				account.getRole(), account.getCreatedAt());
	}
}
