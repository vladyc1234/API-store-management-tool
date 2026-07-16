package com.gamestore.game_store_api.game;

public class DuplicateGameSkuException extends RuntimeException {

	public DuplicateGameSkuException() {
		super("A game with this SKU already exists");
	}
}
