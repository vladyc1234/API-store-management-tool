package com.gamestore.game_store_api.purchase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseItemRequest(
		@NotNull @Positive Long gameId,
		@NotNull @Positive @Max(100) Integer quantity) {
}
