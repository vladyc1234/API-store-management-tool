package com.gamestore.game_store_api.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiProblems {

	private ApiProblems() {
	}

	public static ProblemDetail create(HttpStatus status, String code, String title, String detail,
			HttpServletRequest request) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("urn:game-store:error:" + code));
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		problem.setProperty("correlationId", correlationId(request));
		problem.setProperty("timestamp", Instant.now().toString());
		return problem;
	}

	public static ProblemDetail validation(String detail, List<ApiValidationError> errors,
			HttpServletRequest request) {
		var problem = create(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed", detail,
				request);
		problem.setProperty("errors", errors);
		return problem;
	}

	private static String correlationId(HttpServletRequest request) {
		var value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
		return value instanceof String correlationId ? correlationId : "unavailable";
	}
}
