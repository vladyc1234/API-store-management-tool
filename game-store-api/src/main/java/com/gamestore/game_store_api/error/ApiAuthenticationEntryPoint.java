package com.gamestore.game_store_api.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter problemWriter;

	public ApiAuthenticationEntryPoint(SecurityProblemWriter problemWriter) {
		this.problemWriter = problemWriter;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException, ServletException {
		problemWriter.write(response, ApiProblems.create(HttpStatus.UNAUTHORIZED, "authentication_required",
				"Authentication required", "A valid bearer token is required to access this resource", request));
	}
}
