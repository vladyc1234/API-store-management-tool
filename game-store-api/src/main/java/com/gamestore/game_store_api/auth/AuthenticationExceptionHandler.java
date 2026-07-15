package com.gamestore.game_store_api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthenticationController.class)
public class AuthenticationExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ProblemDetail handleDuplicateEmail(EmailAlreadyRegisteredException exception) {
		return problem(HttpStatus.CONFLICT, "Email already registered", exception.getMessage());
	}

	@ExceptionHandler(InvalidPasswordException.class)
	ProblemDetail handleInvalidPassword(InvalidPasswordException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid password", exception.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials(BadCredentialsException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage());
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
