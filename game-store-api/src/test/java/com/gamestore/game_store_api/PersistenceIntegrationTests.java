package com.gamestore.game_store_api;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.gamestore.game_store_api.game.Game;
import com.gamestore.game_store_api.game.GameRepository;
import com.gamestore.game_store_api.purchase.Purchase;
import com.gamestore.game_store_api.purchase.PurchaseRepository;
import com.gamestore.game_store_api.purchase.PurchaseStatus;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;
import com.gamestore.game_store_api.user.UserAccountRepository;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PersistenceIntegrationTests {

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsCompletedPurchaseWithHistoricalPriceSnapshot() {
		var buyer = userAccountRepository.save(
				new UserAccount("Buyer@Example.com", "test-password-hash", Role.BUYER));
		var game = gameRepository.save(
				new Game("game-001", "Space Adventure", "Cooperative space game", new BigDecimal("29.99"), 10));

		var purchase = new Purchase(buyer);
		purchase.addItem(game, 2);
		purchase.complete();
		var purchaseId = purchaseRepository.saveAndFlush(purchase).getId();

		game.changePrice(new BigDecimal("39.99"));
		gameRepository.saveAndFlush(game);
		entityManager.clear();

		var savedPurchase = purchaseRepository.findById(purchaseId).orElseThrow();
		assertEquals(PurchaseStatus.COMPLETED, savedPurchase.getStatus());
		assertEquals(new BigDecimal("59.98"), savedPurchase.getTotalAmount());
		assertEquals(new BigDecimal("29.99"), savedPurchase.getItems().getFirst().getUnitPrice());
		assertTrue(userAccountRepository.findByEmailIgnoreCase("BUYER@example.com").isPresent());
		assertEquals(1, purchaseRepository.countByStatus(PurchaseStatus.COMPLETED));
	}

	@Test
	void databaseRejectsDuplicateGameSku() {
		gameRepository.saveAndFlush(
				new Game("duplicate-sku", "First Game", null, new BigDecimal("10.00"), 1));

		assertThrows(DataIntegrityViolationException.class, () -> gameRepository.saveAndFlush(
				new Game("DUPLICATE-SKU", "Second Game", null, new BigDecimal("12.00"), 1)));
	}
}
