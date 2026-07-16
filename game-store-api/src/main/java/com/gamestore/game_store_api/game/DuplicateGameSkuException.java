package com.gamestore.game_store_api.game;

public class DuplicateGameSkuException extends com.gamestore.game_store_api.common.StoreDomainException.Conflict {

	public DuplicateGameSkuException() {
		super("A game with this SKU already exists");
	}
}
