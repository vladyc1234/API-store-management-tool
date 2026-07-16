package com.gamestore.game_store_api.game;

public class GameNotFoundException extends com.gamestore.game_store_api.common.StoreDomainException.NotFound {

	public GameNotFoundException(long id) {
		super("Game " + id + " was not found");
	}
}
