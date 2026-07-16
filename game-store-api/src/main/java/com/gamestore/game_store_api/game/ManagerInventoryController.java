package com.gamestore.game_store_api.game;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/manager/inventory")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerInventoryController {

	private final InventoryService inventoryService;

	public ManagerInventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping
	public InventoryPage inventory(
			@RequestParam(required = false) @Size(max = 100) String query,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return inventoryService.inventory(query, page, size);
	}

	@GetMapping("/summary")
	public InventorySummaryResponse summary(
			@RequestParam(defaultValue = "5") @Min(0) @Max(1_000_000) int lowStockThreshold) {
		return inventoryService.summary(lowStockThreshold);
	}
}
