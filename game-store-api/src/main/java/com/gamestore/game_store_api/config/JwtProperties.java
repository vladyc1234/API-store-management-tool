package com.gamestore.game_store_api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secretBase64, String issuer, String audience, Duration accessTokenTtl) {

	public JwtProperties {
		if (secretBase64 == null || secretBase64.isBlank()) {
			throw new IllegalArgumentException("JWT secret must not be blank");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("JWT issuer must not be blank");
		}
		if (audience == null || audience.isBlank()) {
			throw new IllegalArgumentException("JWT audience must not be blank");
		}
		if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
			throw new IllegalArgumentException("JWT access token TTL must be positive");
		}
	}
}
