package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ChangePriceRequest(
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
