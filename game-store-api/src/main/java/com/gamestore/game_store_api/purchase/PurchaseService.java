package com.gamestore.game_store_api.purchase;

import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamestore.game_store_api.game.Game;
import com.gamestore.game_store_api.game.GameRepository;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;
import com.gamestore.game_store_api.user.UserAccountRepository;

@Service
public class PurchaseService {

	private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

	private final PurchaseRepository purchaseRepository;
	private final GameRepository gameRepository;
	private final UserAccountRepository userAccountRepository;

	public PurchaseService(PurchaseRepository purchaseRepository, GameRepository gameRepository,
			UserAccountRepository userAccountRepository) {
		this.purchaseRepository = purchaseRepository;
		this.gameRepository = gameRepository;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public PurchaseResponse purchase(long buyerId, CreatePurchaseRequest request) {
		var buyer = requireActiveBuyer(buyerId);
		validateUniqueGames(request);

		var lockedGames = new HashMap<Long, Game>();
		request.items().stream()
				.map(PurchaseItemRequest::gameId)
				.sorted()
				.forEach(gameId -> lockedGames.put(gameId, gameRepository.findByIdForUpdate(gameId)
						.orElseThrow(() -> new PurchaseGameNotFoundException(gameId))));

		for (var requestedItem : request.items()) {
			var game = lockedGames.get(requestedItem.gameId());
			if (!game.isActive()) {
				throw new PurchaseConflictException("Game " + game.getId() + " is no longer available");
			}
			if (game.getStockQuantity() < requestedItem.quantity()) {
				throw new PurchaseConflictException("Insufficient stock for game " + game.getId());
			}
		}

		var purchase = new Purchase(buyer);
		for (var requestedItem : request.items()) {
			var game = lockedGames.get(requestedItem.gameId());
			game.decreaseStock(requestedItem.quantity());
			purchase.addItem(game, requestedItem.quantity());
		}
		purchase.complete();

		var savedPurchase = purchaseRepository.saveAndFlush(purchase);
		log.info("Completed purchase id {} for buyer id {} with {} items and total {}",
				savedPurchase.getId(), buyerId, savedPurchase.getItems().size(), savedPurchase.getTotalAmount());
		return PurchaseResponse.from(savedPurchase);
	}

	@Transactional(readOnly = true)
	public PurchaseHistoryPage history(long buyerId, int page, int size) {
		requireActiveBuyer(buyerId);
		var sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		return PurchaseHistoryPage.from(
				purchaseRepository.findByBuyerId(buyerId, PageRequest.of(page, size, sort)));
	}

	@Transactional(readOnly = true)
	public PurchaseResponse findPurchase(long buyerId, long purchaseId) {
		requireActiveBuyer(buyerId);
		return PurchaseResponse.from(purchaseRepository.findDetailedByIdAndBuyerId(purchaseId, buyerId)
				.orElseThrow(() -> new PurchaseNotFoundException(purchaseId)));
	}

	private UserAccount requireActiveBuyer(long buyerId) {
		return userAccountRepository.findById(buyerId)
				.filter(UserAccount::isEnabled)
				.filter(account -> account.getRole() == Role.BUYER)
				.orElseThrow(PurchaseAccessException::new);
	}

	private static void validateUniqueGames(CreatePurchaseRequest request) {
		var gameIds = new HashSet<Long>();
		for (var item : request.items()) {
			if (!gameIds.add(item.gameId())) {
				throw new InvalidPurchaseRequestException("Each game can appear only once in a purchase");
			}
		}
	}
}
