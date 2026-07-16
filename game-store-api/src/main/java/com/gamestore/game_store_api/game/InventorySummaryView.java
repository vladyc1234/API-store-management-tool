package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

public interface InventorySummaryView {

	long getActiveGameCount();

	long getTotalUnits();

	long getOutOfStockGameCount();

	long getLowStockGameCount();

	BigDecimal getInventoryValue();
}
