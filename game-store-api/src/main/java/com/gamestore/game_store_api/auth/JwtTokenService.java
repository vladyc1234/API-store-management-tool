package com.gamestore.game_store_api.auth;

import java.time.Clock;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.gamestore.game_store_api.config.JwtProperties;
import com.gamestore.game_store_api.user.UserAccount;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public IssuedToken issueFor(UserAccount account) {
		var issuedAt = clock.instant();
		var expiresAt = issuedAt.plus(properties.accessTokenTtl());
		var claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(account.getEmail())
				.claim("userId", account.getId())
				.claim("roles", List.of(account.getRole().name()))
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256)
				.type("JWT")
				.build();
		var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
		return new IssuedToken(token.getTokenValue(), expiresAt);
	}
}
