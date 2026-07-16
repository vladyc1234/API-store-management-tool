package com.gamestore.game_store_api.game;

import java.util.List;

import org.springframework.data.domain.Page;

public record GameCatalogPage(
		List<GameResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static GameCatalogPage from(Page<Game> games) {
		return new GameCatalogPage(
				games.stream().map(GameResponse::from).toList(),
				games.getNumber(),
				games.getSize(),
				games.getTotalElements(),
				games.getTotalPages());
	}
}
