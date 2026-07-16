package com.gamestore.game_store_api.auth;

import java.time.Instant;

public record TokenResponse(String tokenType, String accessToken, long expiresIn, Instant expiresAt) {

	static TokenResponse from(IssuedToken token) {
		return new TokenResponse("Bearer", token.value(), token.expiresInSeconds(), token.expiresAt());
	}
}
