package com.gamestore.game_store_api.auth;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
}
