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

@Validated
@RestController
@RequestMapping("/api/manager/games")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerGameController {

	private final GameService gameService;

	public ManagerGameController(GameService gameService) {
		this.gameService = gameService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GameResponse create(@Valid @RequestBody CreateGameRequest request) {
		return gameService.create(request);
	}

	@PatchMapping("/{id}/price")
	public GameResponse changePrice(@PathVariable @Positive long id,
			@Valid @RequestBody ChangePriceRequest request) {
		return gameService.changePrice(id, request);
	}

	@PatchMapping("/{id}/stock")
	public GameResponse changeStock(@PathVariable @Positive long id,
			@Valid @RequestBody ChangeStockRequest request) {
		return gameService.changeStock(id, request);
	}

	@DeleteMapping("/{id}")
	public GameResponse deactivate(@PathVariable @Positive long id) {
		return gameService.deactivate(id);
	}
}
