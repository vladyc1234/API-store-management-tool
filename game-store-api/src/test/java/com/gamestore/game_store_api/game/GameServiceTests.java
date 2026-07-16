package com.gamestore.game_store_api.game;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

	@Mock
	private GameRepository gameRepository;

	private GameService gameService;

	@BeforeEach
	void setUp() {
		gameService = new GameService(gameRepository);
	}

	@Test
	void createsAndNormalizesGameThroughRepository() {
		when(gameRepository.existsBySkuIgnoreCase(" game-101 ")).thenReturn(false);
		when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = gameService.create(new CreateGameRequest(
				" game-101 ", " Strategy Game ", "  Tactical campaign  ", new BigDecimal("19.99"), 4));

		assertEquals("GAME-101", response.sku());
		assertEquals("Strategy Game", response.title());
		assertEquals("Tactical campaign", response.description());
		assertEquals(new BigDecimal("19.99"), response.price());
		assertEquals(4, response.stockQuantity());
		verify(gameRepository).saveAndFlush(any(Game.class));
	}

	@Test
	void rejectsDuplicateSkuBeforeSaving() {
		when(gameRepository.existsBySkuIgnoreCase("DUPLICATE")).thenReturn(true);

		assertThrows(DuplicateGameSkuException.class, () -> gameService.create(new CreateGameRequest(
				"DUPLICATE", "Duplicate", null, new BigDecimal("10.00"), 1)));

		verify(gameRepository, never()).saveAndFlush(any(Game.class));
	}

	@Test
	void convertsInsufficientStockIntoDomainConflictWithoutFlushing() {
		var game = new Game("STOCK-1", "Stock Test", null, new BigDecimal("8.00"), 2);
		when(gameRepository.findById(42L)).thenReturn(Optional.of(game));

		var exception = assertThrows(GameConflictException.class,
				() -> gameService.changeStock(42L, new ChangeStockRequest(-3)));

		assertEquals("Stock adjustment would produce an invalid quantity", exception.getMessage());
		assertEquals(2, game.getStockQuantity());
		verify(gameRepository, never()).flush();
	}

	@Test
	void deactivationIsIdempotent() {
		var game = new Game("OFF-1", "Deactivate Test", null, new BigDecimal("5.00"), 1);
		when(gameRepository.findById(7L)).thenReturn(Optional.of(game));

		var first = gameService.deactivate(7L);
		var second = gameService.deactivate(7L);

		assertFalse(first.active());
		assertFalse(second.active());
		verify(gameRepository).flush();
	}
}
