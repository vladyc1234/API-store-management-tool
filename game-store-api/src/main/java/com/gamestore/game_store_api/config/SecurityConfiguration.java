package com.gamestore.game_store_api.config;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.gamestore.game_store_api.error.ApiAccessDeniedHandler;
import com.gamestore.game_store_api.error.ApiAuthenticationEntryPoint;
import com.gamestore.game_store_api.user.Role;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			ApiAuthenticationEntryPoint authenticationEntryPoint, ApiAccessDeniedHandler accessDeniedHandler)
			throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.requestCache(AbstractHttpConfigurer::disable)
				.securityContext(securityContext -> securityContext.requireExplicitSave(true))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/manager/**").hasRole(Role.MANAGER.name())
						.requestMatchers("/api/buyer/**").hasRole(Role.BUYER.name())
						.requestMatchers("/api/catalog/**")
								.hasAnyRole(Role.BUYER.name(), Role.MANAGER.name())
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		byte[] secret;
		try {
			secret = Base64.getDecoder().decode(properties.secretBase64());
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must contain valid Base64", exception);
		}
		if (secret.length < 32) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 256 bits");
		}
		return new SecretKeySpec(secret, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey secretKey) {
		return NimbusJwtEncoder.withSecretKey(secretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties) {
		var decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		var authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
