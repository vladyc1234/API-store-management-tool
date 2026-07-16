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

@Validated
@RestController
@RequestMapping("/api/buyer/purchases")
@PreAuthorize("hasRole('BUYER')")
public class BuyerPurchaseController {

	private final PurchaseService purchaseService;

	public BuyerPurchaseController(PurchaseService purchaseService) {
		this.purchaseService = purchaseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PurchaseResponse purchase(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreatePurchaseRequest request) {
		return purchaseService.purchase(userId(jwt), request);
	}

	@GetMapping
	public PurchaseHistoryPage history(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return purchaseService.history(userId(jwt), page, size);
	}

	@GetMapping("/{purchaseId}")
	public PurchaseResponse findPurchase(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Positive long purchaseId) {
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
