package com.gamestore.game_store_api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@Tag(name = "Authentication")
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	public AuthenticationController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a buyer", description = "Creates an enabled BUYER account and hashes the supplied password.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Buyer registered"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "409", description = "Email is already registered")
	})
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(authenticationService.registerBuyer(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Log in", description = "Validates buyer or manager credentials and returns a one-hour bearer JWT.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Credentials accepted"),
			@ApiResponse(responseCode = "400", description = "Request validation failed"),
			@ApiResponse(responseCode = "401", description = "Credentials are invalid")
	})
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.from(authenticationService.login(request));
	}
}
