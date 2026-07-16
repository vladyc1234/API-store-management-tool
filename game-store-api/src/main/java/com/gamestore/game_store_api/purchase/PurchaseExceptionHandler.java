package com.gamestore.game_store_api.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BuyerPurchaseController.class)
public class PurchaseExceptionHandler {

	@ExceptionHandler({PurchaseNotFoundException.class, PurchaseGameNotFoundException.class})
	ProblemDetail handleNotFound(RuntimeException exception) {
		return problem(HttpStatus.NOT_FOUND, "Purchase resource not found", exception.getMessage());
	}

	@ExceptionHandler(InvalidPurchaseRequestException.class)
	ProblemDetail handleInvalidRequest(InvalidPurchaseRequestException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid purchase request", exception.getMessage());
	}

	@ExceptionHandler(PurchaseAccessException.class)
	ProblemDetail handleAccess(PurchaseAccessException exception) {
		return problem(HttpStatus.FORBIDDEN, "Purchase access denied", exception.getMessage());
	}

	@ExceptionHandler(PurchaseConflictException.class)
	ProblemDetail handleConflict(PurchaseConflictException exception) {
		return problem(HttpStatus.CONFLICT, "Purchase conflict", exception.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
		return problem(HttpStatus.CONFLICT, "Concurrent inventory update",
				"Inventory changed while the purchase was being processed; retry with fresh data");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
