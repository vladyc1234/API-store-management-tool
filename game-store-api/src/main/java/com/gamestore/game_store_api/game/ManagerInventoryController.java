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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.gamestore.game_store_api.config.OpenApiConfiguration;

@Validated
@RestController
@RequestMapping({"/api/v1/manager/inventory", "/api/manager/inventory"})
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Manager inventory")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Request parameters are invalid"),
		@ApiResponse(responseCode = "401", description = "A valid bearer token is required"),
		@ApiResponse(responseCode = "403", description = "MANAGER role is required")
})
public class ManagerInventoryController {

	private final InventoryService inventoryService;

	public ManagerInventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping
	@Operation(summary = "Search all inventory",
			description = "Returns active and inactive games with stock value, ordered by title and ID.")
	public InventoryPage inventory(
			@Parameter(description = "Optional title or SKU fragment")
			@RequestParam(required = false) @Size(max = 100) String query,
			@Parameter(description = "Inclusive quantity considered low stock")
			@RequestParam(defaultValue = "5") @Min(0) @Max(1_000_000) int lowStockThreshold,
			@Parameter(description = "Zero-based page number")
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@Parameter(description = "Page size from 1 to 100")
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return inventoryService.inventory(query, lowStockThreshold, page, size);
	}

	@GetMapping("/summary")
	@Operation(summary = "Summarize active inventory",
			description = "Calculates active/inactive counts, total units, low and empty stock counts, and inventory value.")
	public InventorySummaryResponse summary(
			@Parameter(description = "Inclusive quantity considered low stock")
			@RequestParam(defaultValue = "5") @Min(0) @Max(1_000_000) int lowStockThreshold) {
		return inventoryService.summary(lowStockThreshold);
	}
}
