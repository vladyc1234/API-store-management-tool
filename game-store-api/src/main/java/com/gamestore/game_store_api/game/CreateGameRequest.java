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
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@NotNull @PositiveOrZero Integer stockQuantity) {
}
