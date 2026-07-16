package com.gamestore.game_store_api.game;

import java.util.List;

import org.springframework.data.domain.Page;

public record InventoryPage(
		List<InventoryGameResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static InventoryPage from(Page<Game> games, int lowStockThreshold) {
		return new InventoryPage(
				games.stream().map(game -> InventoryGameResponse.from(game, lowStockThreshold)).toList(),
				games.getNumber(),
				games.getSize(),
				games.getTotalElements(),
				games.getTotalPages());
	}
}
