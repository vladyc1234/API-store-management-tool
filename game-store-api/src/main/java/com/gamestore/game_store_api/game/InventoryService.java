package com.gamestore.game_store_api.game;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

	private final GameRepository gameRepository;

	public InventoryService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Transactional(readOnly = true)
	public InventoryPage inventory(String query, int page, int size) {
		var normalizedQuery = query == null ? "" : query.trim();
		var sort = Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
		return InventoryPage.from(
				gameRepository.searchInventory(normalizedQuery, PageRequest.of(page, size, sort)));
	}

	@Transactional(readOnly = true)
	public InventorySummaryResponse summary(int lowStockThreshold) {
		var summary = gameRepository.summarizeActiveInventory(lowStockThreshold);
		return InventorySummaryResponse.from(summary, gameRepository.countByActiveFalse(), lowStockThreshold);
	}
}
