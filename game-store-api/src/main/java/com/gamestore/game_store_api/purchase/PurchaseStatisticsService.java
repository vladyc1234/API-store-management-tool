package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseStatisticsService {

	private static final Logger log = LoggerFactory.getLogger(PurchaseStatisticsService.class);

	private final PurchaseRepository purchaseRepository;

	public PurchaseStatisticsService(PurchaseRepository purchaseRepository) {
		this.purchaseRepository = purchaseRepository;
	}

	@Transactional(readOnly = true)
	public PurchaseStatisticsResponse statistics(LocalDate from, LocalDate to, int topLimit) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new InvalidStatisticsRangeException("The from date must not be after the to date");
		}

		var fromDate = from == null ? null : from.atStartOfDay();
		var toDateExclusive = toExclusive(to);
		var summary = purchaseRepository.summarizePurchases(
				PurchaseStatus.COMPLETED, fromDate, toDateExclusive);
		var purchaseCount = summary.getPurchaseCount();
		var totalRevenue = defaultMoney(summary.getTotalRevenue());
		var unitsSold = purchaseRepository.sumUnitsSold(
				PurchaseStatus.COMPLETED, fromDate, toDateExclusive);
		var averageOrderValue = purchaseCount == 0
				? BigDecimal.ZERO.setScale(2)
				: totalRevenue.divide(BigDecimal.valueOf(purchaseCount), 2, RoundingMode.HALF_UP);
		var topGames = purchaseRepository.findTopSellingGames(
				PurchaseStatus.COMPLETED, fromDate, toDateExclusive, PageRequest.of(0, topLimit))
				.stream()
				.map(TopGameSalesResponse::from)
				.toList();

		log.debug("Calculated purchase statistics from {} to {} for {} completed purchases",
				from, to, purchaseCount);
		return new PurchaseStatisticsResponse(
				from,
				to,
				purchaseCount,
				totalRevenue,
				unitsSold == null ? 0 : unitsSold,
				summary.getUniqueBuyerCount(),
				averageOrderValue,
				topGames);
	}

	private static java.time.LocalDateTime toExclusive(LocalDate to) {
		if (to == null) {
			return null;
		}
		try {
			return to.plusDays(1).atStartOfDay();
		}
		catch (DateTimeException exception) {
			throw new InvalidStatisticsRangeException("The to date is outside the supported range");
		}
	}

	private static BigDecimal defaultMoney(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(2) : value;
	}
}
