package com.gamestore.game_store_api.auth;

public class EmailAlreadyRegisteredException extends com.gamestore.game_store_api.common.StoreDomainException.Conflict {

	public EmailAlreadyRegisteredException() {
		super("An account with this email already exists");
	}
}
