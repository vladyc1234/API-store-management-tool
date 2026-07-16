package com.gamestore.game_store_api.purchase;

import java.util.List;

import org.springframework.data.domain.Page;

public record PurchaseHistoryPage(
		List<PurchaseSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static PurchaseHistoryPage from(Page<Purchase> purchases) {
		return new PurchaseHistoryPage(
				purchases.stream().map(PurchaseSummaryResponse::from).toList(),
				purchases.getNumber(),
				purchases.getSize(),
				purchases.getTotalElements(),
				purchases.getTotalPages());
	}
}
