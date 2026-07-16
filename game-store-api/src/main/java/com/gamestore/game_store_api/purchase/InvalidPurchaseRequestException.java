package com.gamestore.game_store_api.purchase;

public class InvalidPurchaseRequestException extends RuntimeException {

	public InvalidPurchaseRequestException(String message) {
		super(message);
	}
}
