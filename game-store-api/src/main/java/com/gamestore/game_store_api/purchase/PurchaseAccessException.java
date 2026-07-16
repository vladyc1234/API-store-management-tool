package com.gamestore.game_store_api.purchase;

public class PurchaseAccessException extends com.gamestore.game_store_api.common.StoreDomainException.Forbidden {

	public PurchaseAccessException() {
		super("The authenticated account cannot make or view buyer purchases");
	}
}
