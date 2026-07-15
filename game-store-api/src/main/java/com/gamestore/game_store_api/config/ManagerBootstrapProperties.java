package com.gamestore.game_store_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap")
public record ManagerBootstrapProperties(String managerEmail, String managerPassword) {

	public ManagerBootstrapProperties {
		if (managerEmail == null || managerEmail.isBlank()) {
			throw new IllegalArgumentException("Manager email must not be blank");
		}
		if (managerPassword == null || managerPassword.isBlank()) {
			throw new IllegalArgumentException("Manager password must not be blank");
		}
	}
}
