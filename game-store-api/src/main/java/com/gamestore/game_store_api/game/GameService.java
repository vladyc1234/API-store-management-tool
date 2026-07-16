package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

	private static final Logger log = LoggerFactory.getLogger(GameService.class);

	private final GameRepository gameRepository;

	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Transactional(readOnly = true)
	public GameCatalogPage searchCatalog(String query, String genre, String platform,
			BigDecimal minimumPrice, BigDecimal maximumPrice, Boolean active,
			String sortBy, String direction, int page, int size) {
		if (minimumPrice != null && maximumPrice != null && minimumPrice.compareTo(maximumPrice) > 0) {
			throw new InvalidGameSearchException("minimumPrice must not exceed maximumPrice");
		}
		var property = switch (sortBy == null ? "title" : sortBy.toLowerCase(java.util.Locale.ROOT)) {
			case "title" -> "title";
			case "price" -> "price";
			case "created", "createdat", "creationtime" -> "createdAt";
			default -> throw new InvalidGameSearchException("sort must be title, price, or createdAt");
		};
		var sortDirection = switch (direction == null ? "asc" : direction.toLowerCase(java.util.Locale.ROOT)) {
			case "asc" -> Sort.Direction.ASC;
			case "desc" -> Sort.Direction.DESC;
			default -> throw new InvalidGameSearchException("direction must be asc or desc");
		};
		var sort = Sort.by(new Sort.Order(sortDirection, property), Sort.Order.asc("id"));
		return GameCatalogPage.from(gameRepository.findAll(
				GameSpecifications.catalog(query, genre, platform, minimumPrice, maximumPrice, active),
				PageRequest.of(page, size, sort)));
	}

	@Transactional(readOnly = true)
	public GameResponse findActiveGame(long id) {
		return GameResponse.from(gameRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new GameNotFoundException(id)));
	}

	@Transactional
	public GameResponse create(CreateGameRequest request) {
		if (gameRepository.existsBySkuIgnoreCase(request.sku())) {
			throw new DuplicateGameSkuException();
		}

		var genre = request.genre() == null || request.genre().isBlank() ? "Uncategorized" : request.genre();
		var platform = request.platform() == null || request.platform().isBlank() ? "Unknown" : request.platform();
		var game = new Game(request.sku(), request.title(), request.description(), genre,
				platform, request.price(), request.stockQuantity());
		try {
			var savedGame = gameRepository.saveAndFlush(game);
			log.info("Created game {} with id {} and initial stock {}",
					savedGame.getSku(), savedGame.getId(), savedGame.getStockQuantity());
			return GameResponse.from(savedGame);
		}
		catch (DataIntegrityViolationException exception) {
			throw new DuplicateGameSkuException();
		}
	}

	@Transactional
	public GameResponse changePrice(long id, ChangePriceRequest request) {
		var game = findManagedGame(id);
		ensureActive(game);
		var previousPrice = game.getPrice();
		game.changePrice(request.price());
		gameRepository.flush();
		log.info("Changed price for game id {} from {} to {}", id, previousPrice, game.getPrice());
		return GameResponse.from(game);
	}

	@Transactional
	public GameResponse changeStock(long id, ChangeStockRequest request) {
		var game = findManagedGame(id);
		ensureActive(game);
		var previousStock = game.getStockQuantity();
		if (request.stockQuantity() != null) {
			game.setStockQuantity(request.stockQuantity());
		}
		else {
			try {
				game.setStockQuantity(Math.addExact(previousStock, request.delta()));
			}
			catch (IllegalArgumentException | ArithmeticException exception) {
				throw new GameConflictException("Stock adjustment would produce an invalid quantity");
			}
		}
		gameRepository.flush();
		log.info("Changed stock for game id {} from {} to {}", id, previousStock, game.getStockQuantity());
		return GameResponse.from(game);
	}

	@Transactional
	public void deactivate(long id) {
		var game = findManagedGame(id);
		if (game.isActive()) {
			game.deactivate();
			gameRepository.flush();
			log.info("Deactivated game id {}", id);
		}
	}

	private Game findManagedGame(long id) {
		return gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
	}

	private static void ensureActive(Game game) {
		if (!game.isActive()) {
			throw new GameConflictException("A deactivated game cannot be modified");
		}
	}
}
