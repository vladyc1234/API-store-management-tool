package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

public record InventoryGameResponse(
		Long id,
		String sku,
		String title,
		BigDecimal price,
		int stockQuantity,
		BigDecimal inventoryValue,
		boolean active,
		long version) {

	static InventoryGameResponse from(Game game) {
		return new InventoryGameResponse(
				game.getId(),
				game.getSku(),
				game.getTitle(),
				game.getPrice(),
				game.getStockQuantity(),
				game.getPrice().multiply(BigDecimal.valueOf(game.getStockQuantity())),
				game.isActive(),
				game.getVersion());
	}
}
