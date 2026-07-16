package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseStatisticsResponse(
		LocalDate from,
		LocalDate to,
		long completedPurchases,
		BigDecimal totalRevenue,
		long unitsSold,
		long uniqueBuyers,
		BigDecimal averageOrderValue,
		List<TopGameSalesResponse> topGames) {
}
