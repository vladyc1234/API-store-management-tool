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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

@Validated
@RestController
@RequestMapping("/api/catalog/games")
@PreAuthorize("hasAnyRole('BUYER', 'MANAGER')")
@Tag(name = "Catalog")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Request parameters are invalid"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "The authenticated role is not allowed")
})
public class CatalogController {

	private final GameService gameService;

	public CatalogController(GameService gameService) {
		this.gameService = gameService;
	}

	@GetMapping
	@Operation(summary = "Search the active game catalog",
			description = "Returns active games ordered by title and ID. The optional query matches title or SKU case-insensitively.")
	public GameCatalogPage search(
			@Parameter(description = "Optional title or SKU fragment")
			@RequestParam(required = false) @Size(max = 100) String query,
			@Parameter(description = "Zero-based page number")
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@Parameter(description = "Page size from 1 to 100")
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return gameService.searchCatalog(query, page, size);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Find an active game", description = "Returns one active catalog game by database ID.")
	@ApiResponse(responseCode = "404", description = "Active game not found")
	public GameResponse findById(@Parameter(description = "Game ID") @PathVariable @Positive long id) {
		return gameService.findActiveGame(id);
	}
}
