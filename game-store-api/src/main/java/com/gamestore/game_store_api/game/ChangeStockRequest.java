package com.gamestore.game_store_api.game;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ChangeStockRequest(
		@Min(0) @Max(1_000_000) Integer stockQuantity,
		@Min(-1_000_000) @Max(1_000_000) Integer delta) {

	public ChangeStockRequest(Integer stockQuantity) {
		this(stockQuantity, null);
	}

	@AssertTrue(message = "provide stockQuantity (or legacy delta), but not both")
	public boolean isValidUpdate() {
		return (stockQuantity == null) != (delta == null) && (delta == null || delta != 0);
	}
}
