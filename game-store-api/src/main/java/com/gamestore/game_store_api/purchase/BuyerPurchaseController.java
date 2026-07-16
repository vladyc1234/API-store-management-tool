package com.gamestore.game_store_api.purchase;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

@Validated
@RestController
@RequestMapping("/api/buyer/purchases")
@PreAuthorize("hasRole('BUYER')")
@Tag(name = "Buyer purchases")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Request validation failed"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "BUYER role or purchase ownership is required"),
		@ApiResponse(responseCode = "404", description = "Purchase or game not found"),
		@ApiResponse(responseCode = "409", description = "Stock or concurrent-update conflict")
})
public class BuyerPurchaseController {

	private final PurchaseService purchaseService;

	public BuyerPurchaseController(PurchaseService purchaseService) {
		this.purchaseService = purchaseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Purchase games",
			description = "Atomically validates active stock, deducts units, snapshots current prices, and completes the purchase.")
	public PurchaseResponse purchase(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreatePurchaseRequest request) {
		return purchaseService.purchase(userId(jwt), request);
	}

	@GetMapping
	@Operation(summary = "List purchase history", description = "Returns the authenticated buyer's completed purchases newest first.")
	public PurchaseHistoryPage history(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Zero-based page number")
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@Parameter(description = "Page size from 1 to 100")
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return purchaseService.history(userId(jwt), page, size);
	}

	@GetMapping("/{purchaseId}")
	@Operation(summary = "Get a purchase", description = "Returns a purchase only when it belongs to the authenticated buyer.")
	public PurchaseResponse findPurchase(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Purchase ID") @PathVariable @Positive long purchaseId) {
		return purchaseService.findPurchase(userId(jwt), purchaseId);
	}

	private static long userId(Jwt jwt) {
		Object claim = jwt.getClaim("userId");
		if (claim instanceof Number number) {
			return number.longValue();
		}
		throw new PurchaseAccessException();
	}
}
