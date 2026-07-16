package com.gamestore.game_store_api.purchase;

public class PurchaseConflictException extends com.gamestore.game_store_api.common.StoreDomainException.Conflict {

	public PurchaseConflictException(String message) {
		super(message);
	}
}
