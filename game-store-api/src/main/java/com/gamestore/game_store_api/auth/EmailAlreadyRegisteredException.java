package com.gamestore.game_store_api.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException() {
		super("An account with this email already exists");
	}
}
