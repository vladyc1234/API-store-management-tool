package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(
		Long id,
		PurchaseStatus status,
		BigDecimal totalAmount,
		String currency,
		List<PurchaseItemResponse> items,
		LocalDateTime createdAt) {

	static PurchaseResponse from(Purchase purchase) {
		return new PurchaseResponse(
				purchase.getId(),
				purchase.getStatus(),
				purchase.getTotalAmount(),
				purchase.getCurrency(),
				purchase.getItems().stream().map(PurchaseItemResponse::from).toList(),
				purchase.getCreatedAt());
	}
}
