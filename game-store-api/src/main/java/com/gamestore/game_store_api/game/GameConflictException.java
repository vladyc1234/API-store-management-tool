package com.gamestore.game_store_api.game;

public class GameConflictException extends com.gamestore.game_store_api.common.StoreDomainException.Conflict {

	public GameConflictException(String message) {
		super(message);
	}
}
