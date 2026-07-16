package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;

public record TopGameSalesResponse(
		Long gameId,
		String gameTitle,
		long unitsSold,
		long orderCount,
		BigDecimal revenue) {

	static TopGameSalesResponse from(TopGameSalesView sales) {
		return new TopGameSalesResponse(
				sales.getGameId(),
				sales.getGameTitle(),
				sales.getUnitsSold(),
				sales.getOrderCount(),
				sales.getRevenue());
	}
}
