package com.gamestore.game_store_api.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import com.gamestore.game_store_api.game.Game;
import com.gamestore.game_store_api.game.GameRepository;
import com.gamestore.game_store_api.game.GameSpecifications;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;
import com.gamestore.game_store_api.user.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class GameRepositoryTests {

	private static final String DATABASE_NAME = "game_repository_"
			+ UUID.randomUUID().toString().replace("-", "");

	@DynamicPropertySource
	static void isolatedDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE_NAME
				+ ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void searchesCatalogAndInventoryAndCalculatesActiveInventorySummary() {
		var spaceGame = new Game("SPACE-1", "Space Strategy", null, new BigDecimal("10.00"), 1);
		var puzzleGame = new Game("PUZZLE-1", "Logic Puzzle", null, new BigDecimal("20.00"), 2);
		var retiredGame = new Game("RETIRED-1", "Retired Adventure", null, new BigDecimal("99.00"), 7);
		retiredGame.deactivate();
		gameRepository.saveAllAndFlush(java.util.List.of(spaceGame, puzzleGame, retiredGame));

		var catalogResult = gameRepository.searchActive("space", PageRequest.of(0, 10));
		var inventoryResult = gameRepository.searchInventory("retired", PageRequest.of(0, 10));
		var summary = gameRepository.summarizeActiveInventory(1);

		assertEquals(1, catalogResult.getTotalElements());
		assertEquals("SPACE-1", catalogResult.getContent().getFirst().getSku());
		assertEquals(1, inventoryResult.getTotalElements());
		assertEquals("RETIRED-1", inventoryResult.getContent().getFirst().getSku());
		assertEquals(2, summary.getActiveGameCount());
		assertEquals(3, summary.getTotalUnits());
		assertEquals(0, summary.getOutOfStockGameCount());
		assertEquals(1, summary.getLowStockGameCount());
		assertEquals(0, new BigDecimal("50.00").compareTo(summary.getInventoryValue()));
		assertEquals(1, gameRepository.countByActiveFalse());
	}

	@Test
	void enforcesSkuConstraintAndSupportsCaseInsensitiveLookups() {
		gameRepository.saveAndFlush(new Game("UNIQUE-1", "First", null, new BigDecimal("1.00"), 0));
		assertTrue(gameRepository.findBySkuIgnoreCase("unique-1").isPresent());

		assertThrows(DataIntegrityViolationException.class, () -> gameRepository.saveAndFlush(
				new Game("unique-1", "Second", null, new BigDecimal("2.00"), 0)));
	}

	@Test
	void normalizesEmailForCaseInsensitiveRepositoryLookup() {
		userAccountRepository.saveAndFlush(new UserAccount("Buyer@Test.Example", "hash", Role.BUYER));

		assertTrue(userAccountRepository.findByEmailIgnoreCase("BUYER@test.example").isPresent());
		assertTrue(userAccountRepository.existsByEmailIgnoreCase("buyer@Test.example"));
	}

	@Test
	void composesCatalogSpecificationsForGenrePlatformPriceAndActiveState() {
		gameRepository.saveAllAndFlush(java.util.List.of(
				new Game("SPEC-1", "Space Tactics", null, "Strategy", "PC", new BigDecimal("20.00"), 3),
				new Game("SPEC-2", "Console Tactics", null, "Strategy", "Console", new BigDecimal("25.00"), 3),
				new Game("SPEC-3", "Cheap Tactics", null, "Strategy", "PC", new BigDecimal("5.00"), 3)));

		var result = gameRepository.findAll(GameSpecifications.catalog(
				"tactics", "strategy", "pc", new BigDecimal("10.00"), new BigDecimal("30.00"), true),
				PageRequest.of(0, 10));

		assertEquals(1, result.getTotalElements());
		assertEquals("SPEC-1", result.getContent().getFirst().getSku());
	}
}
