package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;

public record PurchaseItemResponse(
		Long gameId,
		String gameTitle,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal) {

	static PurchaseItemResponse from(PurchaseItem item) {
		return new PurchaseItemResponse(
				item.getGame().getId(),
				item.getGameTitle(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineTotal());
	}
}
