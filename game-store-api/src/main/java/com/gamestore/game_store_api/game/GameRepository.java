package com.gamestore.game_store_api.game;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<Game, Long> {

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
}
