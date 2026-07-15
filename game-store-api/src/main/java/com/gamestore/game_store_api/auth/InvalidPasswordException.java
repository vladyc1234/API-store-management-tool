package com.gamestore.game_store_api.auth;

public class InvalidPasswordException extends RuntimeException {

	public InvalidPasswordException(String message) {
		super(message);
	}
}
