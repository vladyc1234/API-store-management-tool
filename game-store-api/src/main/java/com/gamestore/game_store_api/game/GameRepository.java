package com.gamestore.game_store_api.game;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

	Optional<Game> findBySkuIgnoreCase(String sku);

	boolean existsBySkuIgnoreCase(String sku);

	Page<Game> findByActiveTrue(Pageable pageable);
}
