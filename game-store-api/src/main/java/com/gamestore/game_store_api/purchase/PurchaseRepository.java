package com.gamestore.game_store_api.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	Page<Purchase> findByBuyerId(Long buyerId, Pageable pageable);

	long countByStatus(PurchaseStatus status);
}
