package com.gamestore.game_store_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary security boundary for the bootstrap milestone.
 *
 * <p>The real JWT and role-based rules will replace this configuration when
 * authentication is implemented. Until then, only the health endpoint is public.</p>
 */
@Configuration(proxyBeanMethods = false)
public class BootstrapSecurityConfiguration {

	@Bean
	SecurityFilterChain bootstrapSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.anyRequest().denyAll())
				.build();
	}
}
