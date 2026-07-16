package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(
		@NotBlank @Size(max = 50) String sku,
		@NotBlank @Size(max = 200) String title,
		@Size(max = 5000) String description,
		@Size(max = 100) String genre,
		@Size(max = 100) String platform,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@NotNull @PositiveOrZero Integer stockQuantity) {

	public CreateGameRequest(String sku, String title, String description, BigDecimal price, Integer stockQuantity) {
		this(sku, title, description, "Uncategorized", "Unknown", price, stockQuantity);
	}
}
