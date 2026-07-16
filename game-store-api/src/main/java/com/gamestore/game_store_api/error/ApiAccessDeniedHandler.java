package com.gamestore.game_store_api.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityProblemWriter problemWriter;

	public ApiAccessDeniedHandler(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		problemWriter.write(response, ApiProblems.create(HttpStatus.FORBIDDEN, "access_denied", "Access denied",
				"Your account does not have permission to access this resource", request));
	}
}
