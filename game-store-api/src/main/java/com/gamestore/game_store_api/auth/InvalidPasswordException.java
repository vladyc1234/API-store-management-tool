package com.gamestore.game_store_api.auth;

public class InvalidPasswordException extends com.gamestore.game_store_api.common.StoreDomainException.BadRequest {

	public InvalidPasswordException(String message) {
		super(message);
	}
}
