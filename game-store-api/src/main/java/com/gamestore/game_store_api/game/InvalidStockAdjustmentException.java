package com.gamestore.game_store_api.game;

public class InvalidStockAdjustmentException extends com.gamestore.game_store_api.common.StoreDomainException.BadRequest {

	public InvalidStockAdjustmentException() {
		super("Stock delta must not be zero");
	}
}
