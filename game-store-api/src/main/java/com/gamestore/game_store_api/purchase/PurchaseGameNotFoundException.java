package com.gamestore.game_store_api.purchase;

public class PurchaseGameNotFoundException extends RuntimeException {

	public PurchaseGameNotFoundException(long gameId) {
		super("Game " + gameId + " was not found");
	}
}
