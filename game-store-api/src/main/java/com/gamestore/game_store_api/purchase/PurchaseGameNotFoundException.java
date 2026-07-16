package com.gamestore.game_store_api.purchase;

public class PurchaseGameNotFoundException extends com.gamestore.game_store_api.common.StoreDomainException.NotFound {

	public PurchaseGameNotFoundException(long gameId) {
		super("Game " + gameId + " was not found");
	}
}
