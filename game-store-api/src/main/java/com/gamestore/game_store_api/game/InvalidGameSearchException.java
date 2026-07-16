package com.gamestore.game_store_api.game;

public class InvalidGameSearchException extends com.gamestore.game_store_api.common.StoreDomainException.BadRequest {

	public InvalidGameSearchException(String message) {
		super(message);
	}
}
