package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

public record InventoryGameResponse(
		Long id,
		String sku,
		String title,
		String genre,
		String platform,
		BigDecimal price,
		int stockQuantity,
		boolean lowStock,
		BigDecimal inventoryValue,
		boolean active,
		long version) {

	static InventoryGameResponse from(Game game, int lowStockThreshold) {
		return new InventoryGameResponse(
				game.getId(),
				game.getSku(),
				game.getTitle(),
				game.getGenre(),
				game.getPlatform(),
				game.getPrice(),
				game.getStockQuantity(),
				game.isActive() && game.getStockQuantity() <= lowStockThreshold,
				game.getPrice().multiply(BigDecimal.valueOf(game.getStockQuantity())),
				game.isActive(),
				game.getVersion());
	}
}
