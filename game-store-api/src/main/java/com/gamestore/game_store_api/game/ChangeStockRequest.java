package com.gamestore.game_store_api.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeStockRequest(
		@NotNull @Min(-1_000_000) @Max(1_000_000) Integer delta) {
}
