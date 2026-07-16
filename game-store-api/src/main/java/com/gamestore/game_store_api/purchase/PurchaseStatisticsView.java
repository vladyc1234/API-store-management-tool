package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;

public interface PurchaseStatisticsView {

	long getPurchaseCount();

	BigDecimal getTotalRevenue();

	long getUniqueBuyerCount();
}
