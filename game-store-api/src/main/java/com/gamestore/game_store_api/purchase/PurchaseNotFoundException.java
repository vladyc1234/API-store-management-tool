package com.gamestore.game_store_api.purchase;

public class PurchaseNotFoundException extends RuntimeException {

	public PurchaseNotFoundException(long id) {
		super("Purchase " + id + " was not found");
	}
}
