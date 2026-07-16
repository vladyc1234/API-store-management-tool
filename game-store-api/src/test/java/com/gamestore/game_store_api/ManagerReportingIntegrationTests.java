package com.gamestore.game_store_api;

import java.math.BigDecimal;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.gamestore.game_store_api.config.ManagerBootstrapProperties;
import com.gamestore.game_store_api.game.InventoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ManagerReportingIntegrationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");
	private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");
	private static final String DATABASE_NAME = "manager_reporting_"
			+ UUID.randomUUID().toString().replace("-", "");

	@DynamicPropertySource
	static void isolatedDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE_NAME
				+ ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@LocalServerPort
	private int port;

	@Autowired
	private ManagerBootstrapProperties managerProperties;

	@Autowired
	private InventoryService inventoryService;

	@Test
	void managerCanSearchInventoryAndViewAccurateSummary() throws Exception {
		var managerToken = managerToken();
		var buyerToken = registerAndLoginBuyer();
		var marker = "RPT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var before = inventoryService.summary(5);

		createGame(managerToken, marker + "-OUT", "Inventory Out", "10.00", 0);
		createGame(managerToken, marker + "-LOW", "Inventory Low", "20.00", 3);
		createGame(managerToken, marker + "-FULL", "Inventory Full", "5.00", 10);
		var inactiveId = createGame(managerToken, marker + "-OFF", "Inventory Inactive", "7.00", 4);
		assertEquals(204, request("DELETE", "/api/manager/games/" + inactiveId, managerToken, null).statusCode());

		var after = inventoryService.summary(5);
		assertEquals(3, after.activeGameCount() - before.activeGameCount());
		assertEquals(1, after.inactiveGameCount() - before.inactiveGameCount());
		assertEquals(13, after.totalUnits() - before.totalUnits());
		assertEquals(1, after.outOfStockGameCount() - before.outOfStockGameCount());
		assertEquals(1, after.lowStockGameCount() - before.lowStockGameCount());
		assertEquals(0, new BigDecimal("110.00")
				.compareTo(after.inventoryValue().subtract(before.inventoryValue())));

		var inventory = request("GET", "/api/v1/manager/inventory?query=" + marker + "&size=10&lowStockThreshold=5",
				managerToken, null);
		assertEquals(200, inventory.statusCode());
		assertTrue(inventory.body().contains("\"totalElements\":4"));
		assertTrue(inventory.body().contains(marker + "-OFF"));
		assertTrue(inventory.body().contains("\"active\":false"));
		assertTrue(inventory.body().contains("\"lowStock\":"));
		assertEquals(200,
				request("GET", "/api/manager/inventory/summary?lowStockThreshold=5", managerToken, null)
						.statusCode());
		assertEquals(403, request("GET", "/api/manager/inventory", buyerToken, null).statusCode());
	}

	@Test
	void managerCanViewPurchaseStatisticsTopGamesAndDateFilters() throws Exception {
		var managerToken = managerToken();
		var firstBuyerToken = registerAndLoginBuyer();
		var secondBuyerToken = registerAndLoginBuyer();
		var marker = "STAT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var firstTitle = "Statistics Main Game " + marker;
		var secondTitle = "Statistics Side Game " + marker;
		var firstGameId = createGame(managerToken, marker + "-A", firstTitle, "10.00", 10);
		var secondGameId = createGame(managerToken, marker + "-B", secondTitle, "5.00", 10);

		assertEquals(201, request("POST", "/api/buyer/purchases", firstBuyerToken,
				purchaseJson(firstGameId, 2, secondGameId, 1)).statusCode());
		assertEquals(201, request("POST", "/api/buyer/purchases", secondBuyerToken,
				singleItemPurchaseJson(firstGameId, 1)).statusCode());

		var statistics = request("GET", "/api/v1/manager/statistics/purchases?topLimit=5&lowStockThreshold=5",
				managerToken, null);
		assertEquals(200, statistics.statusCode());
		assertTrue(statistics.body().contains("\"totalOrders\":2"));
		assertTrue(statistics.body().contains("\"currency\":\"EUR\""));
		assertTrue(statistics.body().contains("\"lowStockGameCount\":"));
		assertTrue(statistics.body().contains("\"totalRevenue\":35.00"));
		assertTrue(statistics.body().contains("\"unitsSold\":4"));
		assertTrue(statistics.body().contains("\"uniqueBuyers\":2"));
		assertTrue(statistics.body().contains("\"averageOrderValue\":17.50"));
		assertTrue(statistics.body().contains(firstTitle));
		assertTrue(statistics.body().contains("\"unitsSold\":3"));
		assertTrue(statistics.body().contains("\"revenue\":30.00"));
		assertTrue(statistics.body().contains("\"orderCount\":2"));

		var future = request("GET", "/api/manager/statistics/purchases?from=2099-01-01&topLimit=5",
				managerToken, null);
		assertEquals(200, future.statusCode());
		assertTrue(future.body().contains("\"totalOrders\":0"));
		assertTrue(future.body().contains("\"topGames\":[]"));
		assertEquals(400, request("GET",
				"/api/manager/statistics/purchases?from=2026-02-01&to=2026-01-01",
				managerToken, null).statusCode());
		assertEquals(403,
				request("GET", "/api/manager/statistics/purchases", firstBuyerToken, null).statusCode());
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
		var email = "reporting-" + UUID.randomUUID() + "@example.com";
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

	private static String singleItemPurchaseJson(long gameId, int quantity) {
		return "{\"items\":[{\"gameId\":" + gameId + ",\"quantity\":" + quantity + "}]}";
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
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
