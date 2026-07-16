package com.gamestore.game_store_api.game;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/catalog/games")
@PreAuthorize("hasAnyRole('BUYER', 'MANAGER')")
public class CatalogController {

	private final GameService gameService;

	public CatalogController(GameService gameService) {
		this.gameService = gameService;
	}

	@GetMapping
	public GameCatalogPage search(
			@RequestParam(required = false) @Size(max = 100) String query,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return gameService.searchCatalog(query, page, size);
	}

	@GetMapping("/{id}")
	public GameResponse findById(@PathVariable @Positive long id) {
		return gameService.findActiveGame(id);
	}
}
