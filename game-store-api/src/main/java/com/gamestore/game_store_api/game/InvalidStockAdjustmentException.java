package com.gamestore.game_store_api.game;

public class InvalidStockAdjustmentException extends RuntimeException {

	public InvalidStockAdjustmentException() {
		super("Stock delta must not be zero");
	}
}
