package com.gamestore.game_store_api.error;

public record ApiValidationError(String field, String message) {
}
