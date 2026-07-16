package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseSummaryResponse(
		Long id,
		PurchaseStatus status,
		BigDecimal totalAmount,
		LocalDateTime createdAt) {

	static PurchaseSummaryResponse from(Purchase purchase) {
		return new PurchaseSummaryResponse(
				purchase.getId(),
				purchase.getStatus(),
				purchase.getTotalAmount(),
				purchase.getCreatedAt());
	}
}
