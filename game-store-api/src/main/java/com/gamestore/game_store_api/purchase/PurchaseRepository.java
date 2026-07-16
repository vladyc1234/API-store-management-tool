package com.gamestore.game_store_api.purchase;

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
}
