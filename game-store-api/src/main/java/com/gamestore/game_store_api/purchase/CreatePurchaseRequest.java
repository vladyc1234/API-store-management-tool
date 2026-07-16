package com.gamestore.game_store_api.purchase;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreatePurchaseRequest(
		@NotEmpty @Size(max = 50) List<@Valid PurchaseItemRequest> items) {

	public CreatePurchaseRequest {
		if (items != null) {
			items = List.copyOf(items);
		}
	}
}
