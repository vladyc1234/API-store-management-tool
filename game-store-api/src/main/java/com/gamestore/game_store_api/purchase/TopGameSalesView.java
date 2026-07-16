package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;

public interface TopGameSalesView {

	Long getGameId();

	String getGameTitle();

	long getUnitsSold();

	long getOrderCount();

	BigDecimal getRevenue();
}
