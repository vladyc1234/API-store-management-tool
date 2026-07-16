package com.gamestore.game_store_api.game;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

@Validated
@RestController
@RequestMapping("/api/manager/games")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager games")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Request validation failed"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "MANAGER role is required"),
		@ApiResponse(responseCode = "404", description = "Game not found"),
		@ApiResponse(responseCode = "409", description = "SKU, stock, or concurrent-update conflict")
})
public class ManagerGameController {

	private final GameService gameService;

	public ManagerGameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a game", description = "Adds a new active game with a unique SKU, price, and initial stock.")
	public GameResponse create(@Valid @RequestBody CreateGameRequest request) {
		return gameService.create(request);
	}

	@PatchMapping("/{id}/price")
	@Operation(summary = "Change a game price", description = "Replaces the current price of an active game.")
	public GameResponse changePrice(@Parameter(description = "Game ID") @PathVariable @Positive long id,
			@Valid @RequestBody ChangePriceRequest request) {
		return gameService.changePrice(id, request);
	}

	@PatchMapping("/{id}/stock")
	@Operation(summary = "Adjust game stock",
			description = "Adds or removes units using a non-zero delta. Stock can never become negative.")
	public GameResponse changeStock(@Parameter(description = "Game ID") @PathVariable @Positive long id,
			@Valid @RequestBody ChangeStockRequest request) {
		return gameService.changeStock(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Deactivate a game",
			description = "Makes a game unavailable in the catalog while preserving purchase history. The operation is idempotent.")
	public GameResponse deactivate(@Parameter(description = "Game ID") @PathVariable @Positive long id) {
		return gameService.deactivate(id);
	}
}
