package com.gamestore.game_store_api.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
		info = @Info(
				title = "Game Store Management API",
				version = "v1",
				description = "Backend API for browsing games, purchasing stock, managing inventory, and reviewing sales statistics.",
				contact = @Contact(name = "Game Store API maintainers")),
		tags = {
				@Tag(name = "Authentication", description = "Buyer registration and JWT login"),
				@Tag(name = "Catalog", description = "Active game catalog available to authenticated users"),
				@Tag(name = "Manager games", description = "Manager-only game lifecycle operations"),
				@Tag(name = "Manager inventory", description = "Manager-only inventory search and summaries"),
				@Tag(name = "Buyer purchases", description = "Buyer checkout and purchase history"),
				@Tag(name = "Manager statistics", description = "Manager-only completed-purchase analytics")
		})
@SecurityScheme(
		name = OpenApiConfiguration.BEARER_AUTH,
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "JWT returned by POST /api/auth/login")
public class OpenApiConfiguration {

	public static final String BEARER_AUTH = "bearerAuth";
}
