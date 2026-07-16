package com.gamestore.game_store_api.purchase;

public class InvalidStatisticsRangeException extends com.gamestore.game_store_api.common.StoreDomainException.BadRequest {

	public InvalidStatisticsRangeException(String message) {
		super(message);
	}
}
