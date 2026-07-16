package com.gamestore.game_store_api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.gamestore.game_store_api.config.ManagerBootstrapProperties;
import com.gamestore.game_store_api.game.GameRepository;
import com.gamestore.game_store_api.purchase.PurchaseRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PurchaseIntegrationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");
	private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

	@LocalServerPort
	private int port;

	@Autowired
	private ManagerBootstrapProperties managerProperties;

	@Autowired
	private GameRepository gameRepository;

	@Autowired
	private PurchaseRepository purchaseRepository;

	@Test
	void completesAtomicPurchaseAndReturnsOnlyBuyerHistory() throws Exception {
		var managerToken = managerToken();
		var buyerToken = registerAndLoginBuyer();
		var otherBuyerToken = registerAndLoginBuyer();
		var firstTitle = "Purchase Adventure " + UUID.randomUUID();
		var secondTitle = "Purchase Strategy " + UUID.randomUUID();
		var firstGameId = createGame(managerToken, uniqueSku("BUY-A"), firstTitle, "10.00", 3);
		var secondGameId = createGame(managerToken, uniqueSku("BUY-B"), secondTitle, "5.00", 4);

		var purchase = request("POST", "/api/buyer/purchases", buyerToken,
				purchaseJson(firstGameId, 2, secondGameId, 3));
		assertEquals(201, purchase.statusCode());
		assertTrue(purchase.body().contains("\"status\":\"COMPLETED\""));
		assertTrue(purchase.body().contains("\"totalAmount\":35.00"));
		var purchaseId = id(purchase.body());

		assertEquals(1, gameRepository.findById(firstGameId).orElseThrow().getStockQuantity());
		assertEquals(1, gameRepository.findById(secondGameId).orElseThrow().getStockQuantity());

		assertEquals(200, request("PATCH", "/api/manager/games/" + firstGameId + "/price", managerToken,
				"{\"price\":19.00}").statusCode());
		var detail = request("GET", "/api/buyer/purchases/" + purchaseId, buyerToken, null);
		assertEquals(200, detail.statusCode());
		assertTrue(detail.body().contains(firstTitle));
		assertTrue(detail.body().contains(secondTitle));
		assertTrue(detail.body().contains("\"unitPrice\":10.00"));

		var history = request("GET", "/api/buyer/purchases?page=0&size=10", buyerToken, null);
		assertEquals(200, history.statusCode());
		assertTrue(history.body().contains("\"id\":" + purchaseId));
		assertEquals(404,
				request("GET", "/api/buyer/purchases/" + purchaseId, otherBuyerToken, null).statusCode());
		assertEquals(403, request("GET", "/api/buyer/purchases", managerToken, null).statusCode());
	}

	@Test
	void failedMultiGamePurchaseRollsBackAllStockAndCreatesNoPurchase() throws Exception {
		var managerToken = managerToken();
		var buyerToken = registerAndLoginBuyer();
		var firstGameId = createGame(managerToken, uniqueSku("ROLLBACK-A"), "Rollback Game A", "12.00", 5);
		var secondGameId = createGame(managerToken, uniqueSku("ROLLBACK-B"), "Rollback Game B", "8.00", 1);
		var purchasesBefore = purchaseRepository.count();

		var failedPurchase = request("POST", "/api/buyer/purchases", buyerToken,
				purchaseJson(firstGameId, 2, secondGameId, 2));
		assertEquals(409, failedPurchase.statusCode());
		assertEquals(5, gameRepository.findById(firstGameId).orElseThrow().getStockQuantity());
		assertEquals(1, gameRepository.findById(secondGameId).orElseThrow().getStockQuantity());
		assertEquals(purchasesBefore, purchaseRepository.count());

		var duplicateGameRequest = "{\"items\":[{\"gameId\":" + firstGameId
				+ ",\"quantity\":1},{\"gameId\":" + firstGameId + ",\"quantity\":1}]}";
		assertEquals(400,
				request("POST", "/api/buyer/purchases", buyerToken, duplicateGameRequest).statusCode());
		assertEquals(5, gameRepository.findById(firstGameId).orElseThrow().getStockQuantity());
		assertEquals(purchasesBefore, purchaseRepository.count());
	}

	private long createGame(String managerToken, String sku, String title, String price, int stock) throws Exception {
		var response = request("POST", "/api/manager/games", managerToken,
				"{\"sku\":\"" + sku + "\",\"title\":\"" + title
						+ "\",\"description\":null,\"price\":" + price
						+ ",\"stockQuantity\":" + stock + "}");
		assertEquals(201, response.statusCode());
		return id(response.body());
	}

	private String managerToken() throws Exception {
		var login = request("POST", "/api/v1/auth/login", null,
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword()));
		assertEquals(200, login.statusCode());
		return accessToken(login.body());
	}

	private String registerAndLoginBuyer() throws Exception {
		var email = "purchase-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";
		assertEquals(201, request("POST", "/api/v1/auth/register", null, credentials(email, password)).statusCode());
		var login = request("POST", "/api/v1/auth/login", null, credentials(email, password));
		assertEquals(200, login.statusCode());
		return accessToken(login.body());
	}

	private HttpResponse<String> request(String method, String path, String accessToken, String body) throws Exception {
		var builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path));
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		if (body == null) {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}
		else {
			builder.header("Content-Type", "application/json")
					.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String purchaseJson(long firstGameId, int firstQuantity,
			long secondGameId, int secondQuantity) {
		return "{\"items\":[{\"gameId\":" + firstGameId + ",\"quantity\":" + firstQuantity
				+ "},{\"gameId\":" + secondGameId + ",\"quantity\":" + secondQuantity + "}]}";
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String uniqueSku(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private static String accessToken(String responseBody) {
		var matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain an access token");
		return matcher.group(1);
	}

	private static long id(String responseBody) {
		var matcher = ID_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain an id");
		return Long.parseLong(matcher.group(1));
	}
}

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PurchaseServiceUnitTests {

	@org.mockito.Mock
	private com.gamestore.game_store_api.purchase.PurchaseRepository purchaseRepository;
	@org.mockito.Mock
	private com.gamestore.game_store_api.game.GameRepository gameRepository;
	@org.mockito.Mock
	private com.gamestore.game_store_api.user.UserAccountRepository userRepository;

	@Test
	void purchaseCalculatesTotalReducesStockAndStoresEurSnapshot() {
		var service = new com.gamestore.game_store_api.purchase.PurchaseService(
				purchaseRepository, gameRepository, userRepository);
		var buyer = new com.gamestore.game_store_api.user.UserAccount(
				"buyer@example.com", "hash", "Buyer", com.gamestore.game_store_api.user.Role.BUYER);
		var game = new com.gamestore.game_store_api.game.Game(
				"UNIT-1", "Unit Purchase", null, "Action", "PC", new java.math.BigDecimal("12.50"), 5);
		org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(buyer));
		org.mockito.Mockito.when(gameRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(game));
		org.mockito.Mockito.when(purchaseRepository.saveAndFlush(
				org.mockito.ArgumentMatchers.any(com.gamestore.game_store_api.purchase.Purchase.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.purchase(1L, request(10L, 2));

		org.assertj.core.api.Assertions.assertThat(response.totalAmount()).isEqualByComparingTo("25.00");
		org.assertj.core.api.Assertions.assertThat(response.currency()).isEqualTo("EUR");
		org.assertj.core.api.Assertions.assertThat(game.getStockQuantity()).isEqualTo(3);
	}

	@Test
	void inactiveAndInsufficientGamesCreateNoPurchase() {
		var service = new com.gamestore.game_store_api.purchase.PurchaseService(
				purchaseRepository, gameRepository, userRepository);
		var buyer = new com.gamestore.game_store_api.user.UserAccount(
				"buyer@example.com", "hash", "Buyer", com.gamestore.game_store_api.user.Role.BUYER);
		var inactive = new com.gamestore.game_store_api.game.Game(
				"UNIT-OFF", "Inactive", null, new java.math.BigDecimal("5.00"), 2);
		inactive.deactivate();
		org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(buyer));
		org.mockito.Mockito.when(gameRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(inactive));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.purchase(1L, request(10L, 1)))
				.isInstanceOf(com.gamestore.game_store_api.purchase.PurchaseConflictException.class);
		org.mockito.Mockito.verify(purchaseRepository, org.mockito.Mockito.never())
				.saveAndFlush(org.mockito.ArgumentMatchers.any());
	}

	private static com.gamestore.game_store_api.purchase.CreatePurchaseRequest request(long gameId, int quantity) {
		return new com.gamestore.game_store_api.purchase.CreatePurchaseRequest(java.util.List.of(
				new com.gamestore.game_store_api.purchase.PurchaseItemRequest(gameId, quantity)));
	}
}

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.test.context.SpringBootTest
class PurchaseConcurrencyIntegrationTests {

	private static final String DATABASE_NAME = "purchase_concurrency_"
			+ java.util.UUID.randomUUID().toString().replace("-", "");

	@org.springframework.test.context.DynamicPropertySource
	static void isolatedDatabase(org.springframework.test.context.DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE_NAME
				+ ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@org.springframework.beans.factory.annotation.Autowired
	private com.gamestore.game_store_api.purchase.PurchaseService purchaseService;
	@org.springframework.beans.factory.annotation.Autowired
	private com.gamestore.game_store_api.purchase.PurchaseRepository purchaseRepository;
	@org.springframework.beans.factory.annotation.Autowired
	private com.gamestore.game_store_api.game.GameRepository gameRepository;
	@org.springframework.beans.factory.annotation.Autowired
	private com.gamestore.game_store_api.user.UserAccountRepository userRepository;

	@Test
	void simultaneousBuyersCannotOversellOneRemainingUnit() throws Exception {
		var suffix = java.util.UUID.randomUUID().toString();
		var firstBuyer = userRepository.saveAndFlush(new com.gamestore.game_store_api.user.UserAccount(
				"first-" + suffix + "@example.com", "hash", "First", com.gamestore.game_store_api.user.Role.BUYER));
		var secondBuyer = userRepository.saveAndFlush(new com.gamestore.game_store_api.user.UserAccount(
				"second-" + suffix + "@example.com", "hash", "Second", com.gamestore.game_store_api.user.Role.BUYER));
		var game = gameRepository.saveAndFlush(new com.gamestore.game_store_api.game.Game(
				"RACE-" + suffix.substring(0, 8), "Concurrency Game", null,
				"Strategy", "PC", new java.math.BigDecimal("9.99"), 1));
		var purchasesBefore = purchaseRepository.count();
		var ready = new java.util.concurrent.CountDownLatch(2);
		var start = new java.util.concurrent.CountDownLatch(1);
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);

		try {
			var first = executor.submit(() -> attemptPurchase(firstBuyer.getId(), game.getId(), ready, start));
			var second = executor.submit(() -> attemptPurchase(secondBuyer.getId(), game.getId(), ready, start));
			org.assertj.core.api.Assertions.assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
			start.countDown();

			var outcomes = java.util.List.of(
					first.get(10, java.util.concurrent.TimeUnit.SECONDS),
					second.get(10, java.util.concurrent.TimeUnit.SECONDS));
			org.assertj.core.api.Assertions.assertThat(outcomes).containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}

		org.assertj.core.api.Assertions.assertThat(gameRepository.findById(game.getId()).orElseThrow()
				.getStockQuantity()).isZero();
		org.assertj.core.api.Assertions.assertThat(purchaseRepository.count()).isEqualTo(purchasesBefore + 1);
	}

	private boolean attemptPurchase(long buyerId, long gameId, java.util.concurrent.CountDownLatch ready,
			java.util.concurrent.CountDownLatch start) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			purchaseService.purchase(buyerId, new com.gamestore.game_store_api.purchase.CreatePurchaseRequest(
					java.util.List.of(new com.gamestore.game_store_api.purchase.PurchaseItemRequest(gameId, 1))));
			return true;
		}
		catch (com.gamestore.game_store_api.purchase.PurchaseConflictException exception) {
			return false;
		}
	}
}
