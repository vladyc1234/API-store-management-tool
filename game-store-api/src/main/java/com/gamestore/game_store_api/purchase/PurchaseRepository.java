package com.gamestore.game_store_api.purchase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	Page<Purchase> findByBuyerId(Long buyerId, Pageable pageable);

	@Query("""
			select distinct purchase from Purchase purchase
			left join fetch purchase.items item
			left join fetch item.game
			where purchase.id = :purchaseId and purchase.buyer.id = :buyerId
			""")
	Optional<Purchase> findDetailedByIdAndBuyerId(
			@Param("purchaseId") Long purchaseId,
			@Param("buyerId") Long buyerId);

	long countByStatus(PurchaseStatus status);

	@Query("""
			select count(purchase) as purchaseCount,
			       coalesce(sum(purchase.totalAmount), 0.00) as totalRevenue,
			       count(distinct purchase.buyer.id) as uniqueBuyerCount
			from Purchase purchase
			where purchase.status = :status
			  and (:fromDate is null or purchase.createdAt >= :fromDate)
			  and (:toDateExclusive is null or purchase.createdAt < :toDateExclusive)
			""")
	PurchaseStatisticsView summarizePurchases(
			@Param("status") PurchaseStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);

	@Query("""
			select coalesce(sum(item.quantity), 0)
			from Purchase purchase join purchase.items item
			where purchase.status = :status
			  and (:fromDate is null or purchase.createdAt >= :fromDate)
			  and (:toDateExclusive is null or purchase.createdAt < :toDateExclusive)
			""")
	Long sumUnitsSold(
			@Param("status") PurchaseStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive);

	@Query("""
			select item.game.id as gameId,
			       item.gameTitle as gameTitle,
			       sum(item.quantity) as unitsSold,
			       sum(item.lineTotal) as revenue
			from Purchase purchase join purchase.items item
			where purchase.status = :status
			  and (:fromDate is null or purchase.createdAt >= :fromDate)
			  and (:toDateExclusive is null or purchase.createdAt < :toDateExclusive)
			group by item.game.id, item.gameTitle
			order by sum(item.quantity) desc, sum(item.lineTotal) desc, item.game.id asc
			""")
	List<TopGameSalesView> findTopSellingGames(
			@Param("status") PurchaseStatus status,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDateExclusive") LocalDateTime toDateExclusive,
			Pageable pageable);
}
