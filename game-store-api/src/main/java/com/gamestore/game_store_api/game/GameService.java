package com.gamestore.game_store_api.game;

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
	public GameCatalogPage searchCatalog(String query, int page, int size) {
		var normalizedQuery = query == null ? "" : query.trim();
		var sort = Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
		return GameCatalogPage.from(
				gameRepository.searchActive(normalizedQuery, PageRequest.of(page, size, sort)));
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

		var game = new Game(request.sku(), request.title(), request.description(),
				request.price(), request.stockQuantity());
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
		var delta = request.delta();
		if (delta == 0) {
			throw new InvalidStockAdjustmentException();
		}

		var previousStock = game.getStockQuantity();
		try {
			if (delta > 0) {
				game.increaseStock(delta);
			}
			else {
				game.decreaseStock(-delta);
			}
		}
		catch (IllegalStateException | ArithmeticException exception) {
			throw new GameConflictException("Stock adjustment would produce an invalid quantity");
		}
		gameRepository.flush();
		log.info("Changed stock for game id {} from {} to {}", id, previousStock, game.getStockQuantity());
		return GameResponse.from(game);
	}

	@Transactional
	public GameResponse deactivate(long id) {
		var game = findManagedGame(id);
		if (game.isActive()) {
			game.deactivate();
			gameRepository.flush();
			log.info("Deactivated game id {}", id);
		}
		return GameResponse.from(game);
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
