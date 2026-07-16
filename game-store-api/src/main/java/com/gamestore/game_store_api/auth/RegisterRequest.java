package com.gamestore.game_store_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@Size(max = 100) String displayName,
		@NotBlank
		@Size(min = 6, max = 72)
		@Pattern(
				regexp = "^(?=.*\\p{L})(?=.*[\\p{P}\\p{S}])[^\\r\\n]+$",
				message = "must contain at least one letter and one special character")
		String password) {
}
