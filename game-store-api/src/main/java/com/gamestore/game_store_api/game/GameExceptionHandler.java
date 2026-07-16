package com.gamestore.game_store_api.game;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CatalogController.class, ManagerGameController.class})
public class GameExceptionHandler {

	@ExceptionHandler(GameNotFoundException.class)
	ProblemDetail handleNotFound(GameNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Game not found", exception.getMessage());
	}

	@ExceptionHandler(DuplicateGameSkuException.class)
	ProblemDetail handleDuplicateSku(DuplicateGameSkuException exception) {
		return problem(HttpStatus.CONFLICT, "Duplicate game SKU", exception.getMessage());
	}

	@ExceptionHandler(InvalidStockAdjustmentException.class)
	ProblemDetail handleInvalidStockAdjustment(InvalidStockAdjustmentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid stock adjustment", exception.getMessage());
	}

	@ExceptionHandler(GameConflictException.class)
	ProblemDetail handleGameConflict(GameConflictException exception) {
		return problem(HttpStatus.CONFLICT, "Game update conflict", exception.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
		return problem(HttpStatus.CONFLICT, "Concurrent game update",
				"The game changed while this request was being processed; retry with fresh data");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
