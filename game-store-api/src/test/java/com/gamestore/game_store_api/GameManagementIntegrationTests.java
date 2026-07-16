package com.gamestore.game_store_api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.gamestore.game_store_api.config.ManagerBootstrapProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GameManagementIntegrationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");
	private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

	@LocalServerPort
	private int port;

	@Autowired
	private ManagerBootstrapProperties managerProperties;

	@Test
	void managerCanCreateSearchUpdateStockPriceAndDeactivateGame() throws Exception {
		var managerToken = managerToken();
		var sku = uniqueSku("LIFECYCLE");

		var creation = request("POST", "/api/manager/games", managerToken,
				gameJson(sku, "Lifecycle Strategy Game", "A tactical game", "29.99", 5));
		assertEquals(201, creation.statusCode());
		var gameId = id(creation.body());

		var search = request("GET", "/api/catalog/games?query=" + encode(sku), managerToken, null);
		assertEquals(200, search.statusCode());
		assertTrue(search.body().contains(sku));

		var priceChange = request("PATCH", "/api/manager/games/" + gameId + "/price", managerToken,
				"{\"price\":39.99}");
		assertEquals(200, priceChange.statusCode());
		assertTrue(priceChange.body().contains("\"price\":39.99"));

		var stockIncrease = request("PATCH", "/api/manager/games/" + gameId + "/stock", managerToken,
				"{\"delta\":2}");
		assertEquals(200, stockIncrease.statusCode());
		assertTrue(stockIncrease.body().contains("\"stockQuantity\":7"));

		var stockDecrease = request("PATCH", "/api/manager/games/" + gameId + "/stock", managerToken,
				"{\"delta\":-3}");
		assertEquals(200, stockDecrease.statusCode());
		assertTrue(stockDecrease.body().contains("\"stockQuantity\":4"));

		var deactivation = request("DELETE", "/api/manager/games/" + gameId, managerToken, null);
		assertEquals(200, deactivation.statusCode());
		assertTrue(deactivation.body().contains("\"active\":false"));
		assertEquals(404,
				request("GET", "/api/catalog/games/" + gameId, managerToken, null).statusCode());
		assertEquals(409, request("PATCH", "/api/manager/games/" + gameId + "/price", managerToken,
				"{\"price\":49.99}").statusCode());
	}

	@Test
	void buyerCanSearchCatalogButCannotManageGames() throws Exception {
		var managerToken = managerToken();
		var buyerToken = registerAndLoginBuyer();
		var sku = uniqueSku("CATALOG");
		var title = "Searchable Puzzle Game " + UUID.randomUUID();

		assertEquals(201, request("POST", "/api/manager/games", managerToken,
				gameJson(sku, title, null, "14.50", 8)).statusCode());

		var buyerSearch = request("GET", "/api/catalog/games?query=" + encode(title), buyerToken, null);
		assertEquals(200, buyerSearch.statusCode());
		assertTrue(buyerSearch.body().contains(sku));
		assertEquals(403, request("POST", "/api/manager/games", buyerToken,
				gameJson(uniqueSku("FORBIDDEN"), "Forbidden Game", null, "10.00", 1)).statusCode());
		assertEquals(401, request("GET", "/api/catalog/games", null, null).statusCode());
	}

	@Test
	void rejectsDuplicateSkuAndInvalidStockAdjustments() throws Exception {
		var managerToken = managerToken();
		var sku = uniqueSku("CONSTRAINT");
		var creationBody = gameJson(sku, "Constraint Test Game", null, "20.00", 2);

		var creation = request("POST", "/api/manager/games", managerToken, creationBody);
		assertEquals(201, creation.statusCode());
		var gameId = id(creation.body());

		assertEquals(409, request("POST", "/api/manager/games", managerToken, creationBody).statusCode());
		assertEquals(400, request("PATCH", "/api/manager/games/" + gameId + "/stock", managerToken,
				"{\"delta\":0}").statusCode());
		assertEquals(409, request("PATCH", "/api/manager/games/" + gameId + "/stock", managerToken,
				"{\"delta\":-3}").statusCode());
	}

	private String managerToken() throws Exception {
		var login = request("POST", "/api/auth/login", null,
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword()));
		assertEquals(200, login.statusCode());
		return accessToken(login.body());
	}

	private String registerAndLoginBuyer() throws Exception {
		var email = "catalog-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";
		assertEquals(201, request("POST", "/api/auth/register", null, credentials(email, password)).statusCode());
		var login = request("POST", "/api/auth/login", null, credentials(email, password));
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

	private static String gameJson(String sku, String title, String description, String price, int stock) {
		var descriptionJson = description == null ? "null" : "\"" + description + "\"";
		return "{\"sku\":\"" + sku + "\",\"title\":\"" + title + "\",\"description\":"
				+ descriptionJson + ",\"price\":" + price + ",\"stockQuantity\":" + stock + "}";
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String uniqueSku(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String accessToken(String responseBody) {
		var matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain an access token");
		return matcher.group(1);
	}

	private static long id(String responseBody) {
		var matcher = ID_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain a game id");
		return Long.parseLong(matcher.group(1));
	}
}
