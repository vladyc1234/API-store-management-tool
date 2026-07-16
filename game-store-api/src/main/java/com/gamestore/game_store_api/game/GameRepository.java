package com.gamestore.game_store_api.game;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {

	Optional<Game> findBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCase(String sku);

	Page<Game> findByActiveTrue(Pageable pageable);

	Optional<Game> findByIdAndActiveTrue(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select game from Game game where game.id = :id")
	Optional<Game> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select game from Game game
			where game.active = true
			  and (:query = ''
			       or lower(game.title) like lower(concat('%', :query, '%'))
			       or lower(game.sku) like lower(concat('%', :query, '%')))
			""")
	Page<Game> searchActive(@Param("query") String query, Pageable pageable);

	@Query("""
			select game from Game game
			where :query = ''
			   or lower(game.title) like lower(concat('%', :query, '%'))
			   or lower(game.sku) like lower(concat('%', :query, '%'))
			""")
	Page<Game> searchInventory(@Param("query") String query, Pageable pageable);

	@Query("""
			select count(game) as activeGameCount,
			       coalesce(sum(game.stockQuantity), 0) as totalUnits,
			       coalesce(sum(case when game.stockQuantity = 0 then 1 else 0 end), 0) as outOfStockGameCount,
			       coalesce(sum(case when game.stockQuantity > 0 and game.stockQuantity <= :threshold then 1 else 0 end), 0) as lowStockGameCount,
			       coalesce(sum(game.price * game.stockQuantity), 0.00) as inventoryValue
			from Game game
			where game.active = true
			""")
	InventorySummaryView summarizeActiveInventory(@Param("threshold") int threshold);

	long countByActiveFalse();

	long countByActiveTrueAndStockQuantityLessThanEqual(int threshold);
}
