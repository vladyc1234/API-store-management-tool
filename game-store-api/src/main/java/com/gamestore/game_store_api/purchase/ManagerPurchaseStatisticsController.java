package com.gamestore.game_store_api.purchase;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

@Validated
@RestController
@RequestMapping({"/api/v1/manager/statistics/purchases", "/api/manager/statistics/purchases"})
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager statistics")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Date range or result limit is invalid"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "MANAGER role is required")
})
public class ManagerPurchaseStatisticsController {

	private final PurchaseStatisticsService purchaseStatisticsService;

	public ManagerPurchaseStatisticsController(PurchaseStatisticsService purchaseStatisticsService) {
		this.purchaseStatisticsService = purchaseStatisticsService;
	}

	@GetMapping
	@Operation(summary = "Calculate purchase statistics",
			description = "Aggregates completed purchases using historical line-item prices. Date boundaries are inclusive.")
	public PurchaseStatisticsResponse statistics(
			@Parameter(description = "Optional first purchase date, inclusive", example = "2026-01-01")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Optional last purchase date, inclusive", example = "2026-12-31")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@Parameter(description = "Number of top-selling games from 1 to 50")
			@RequestParam(defaultValue = "5") @Min(1) @Max(50) int topLimit,
			@Parameter(description = "Inclusive quantity considered low stock")
			@RequestParam(defaultValue = "5") @Min(0) @Max(1_000_000) int lowStockThreshold) {
		return purchaseStatisticsService.statistics(from, to, topLimit, lowStockThreshold);
	}
}
