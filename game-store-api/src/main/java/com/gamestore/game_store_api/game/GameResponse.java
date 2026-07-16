package com.gamestore.game_store_api.game;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GameResponse(
		Long id,
		String sku,
		String title,
		String description,
		BigDecimal price,
		int stockQuantity,
		boolean active,
		long version,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	static GameResponse from(Game game) {
		return new GameResponse(
				game.getId(),
				game.getSku(),
				game.getTitle(),
				game.getDescription(),
				game.getPrice(),
				game.getStockQuantity(),
				game.isActive(),
				game.getVersion(),
				game.getCreatedAt(),
				game.getUpdatedAt());
	}
}
