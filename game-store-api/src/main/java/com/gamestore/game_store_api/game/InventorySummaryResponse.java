package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

public record InventorySummaryResponse(
		long activeGameCount,
		long inactiveGameCount,
		long totalUnits,
		long outOfStockGameCount,
		long lowStockGameCount,
		int lowStockThreshold,
		BigDecimal inventoryValue) {

	static InventorySummaryResponse from(InventorySummaryView summary, long inactiveGameCount, int threshold) {
		return new InventorySummaryResponse(
				summary.getActiveGameCount(),
				inactiveGameCount,
				summary.getTotalUnits(),
				summary.getOutOfStockGameCount(),
				summary.getLowStockGameCount(),
				threshold,
				summary.getInventoryValue());
	}
}
