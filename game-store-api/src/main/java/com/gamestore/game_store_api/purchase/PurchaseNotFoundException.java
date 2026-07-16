package com.gamestore.game_store_api.purchase;

public class PurchaseNotFoundException extends com.gamestore.game_store_api.common.StoreDomainException.NotFound {

	public PurchaseNotFoundException(long id) {
		super("Purchase " + id + " was not found");
	}
}
