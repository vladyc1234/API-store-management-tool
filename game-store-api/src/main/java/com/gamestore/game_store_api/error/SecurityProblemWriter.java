package com.gamestore.game_store_api.error;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityProblemWriter {

	private final ObjectMapper objectMapper;

	public SecurityProblemWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
		response.setStatus(problem.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
