package com.gamestore.game_store_api.game;

public class GameNotFoundException extends RuntimeException {

	public GameNotFoundException(long id) {
		super("Game " + id + " was not found");
	}
}
