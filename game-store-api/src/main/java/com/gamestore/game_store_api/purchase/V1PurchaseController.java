package com.gamestore.game_store_api.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/purchases")
@PreAuthorize("hasRole('BUYER')")
@Tag(name = "Buyer purchases v1")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Request validation failed"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "BUYER role is required"),
		@ApiResponse(responseCode = "404", description = "Game not found"),
		@ApiResponse(responseCode = "409", description = "Game is inactive or stock is insufficient")
})
public class V1PurchaseController {

	private final PurchaseService purchaseService;

	public V1PurchaseController(PurchaseService purchaseService) {
		this.purchaseService = purchaseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Purchase one game",
			description = "Locks the game, snapshots its EUR price, reduces stock, and records the purchase atomically.")
	public PurchaseResponse purchase(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody SingleGamePurchaseRequest request) {
		return purchaseService.purchase(userId(jwt), request.toPurchaseRequest());
	}

	@GetMapping("/me")
	@Operation(summary = "List my purchase history",
			description = "Returns only purchases owned by the authenticated buyer, newest first.")
	public PurchaseHistoryPage history(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return purchaseService.history(userId(jwt), page, size);
	}

	private static long userId(Jwt jwt) {
		return switch (jwt.getClaim("userId")) {
			case Number number -> number.longValue();
			case null, default -> throw new PurchaseAccessException();
		};
	}
}
