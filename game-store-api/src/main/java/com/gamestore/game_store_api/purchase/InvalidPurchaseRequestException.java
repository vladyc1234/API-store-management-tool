package com.gamestore.game_store_api.purchase;

public class InvalidPurchaseRequestException extends com.gamestore.game_store_api.common.StoreDomainException.BadRequest {

	public InvalidPurchaseRequestException(String message) {
		super(message);
	}
}
