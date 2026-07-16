package com.gamestore.game_store_api.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ManagerPurchaseStatisticsController.class)
public class PurchaseStatisticsExceptionHandler {

	@ExceptionHandler(InvalidStatisticsRangeException.class)
	ProblemDetail handleInvalidRange(InvalidStatisticsRangeException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid statistics date range");
		return problem;
	}
}
