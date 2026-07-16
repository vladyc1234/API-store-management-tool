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

@Validated
@RestController
@RequestMapping("/api/manager/statistics/purchases")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerPurchaseStatisticsController {

	private final PurchaseStatisticsService purchaseStatisticsService;

	public ManagerPurchaseStatisticsController(PurchaseStatisticsService purchaseStatisticsService) {
		this.purchaseStatisticsService = purchaseStatisticsService;
	}

	@GetMapping
	public PurchaseStatisticsResponse statistics(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) int topLimit) {
		return purchaseStatisticsService.statistics(from, to, topLimit);
	}
}
