package com.gamestore.game_store_api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

@ActiveProfiles("mysql-smoke")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MySqlSmokeIT {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");
	private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":(\\d+)");

	@LocalServerPort
	private int port;

	@Autowired
	private ManagerBootstrapProperties managerProperties;

	@Test
	void completesCriticalBuyerAndManagerJourneyAgainstMySql() throws Exception {
		var health = request("GET", "/actuator/health", null, null);
		assertEquals(200, health.statusCode());
		assertTrue(health.body().contains("\"status\":\"UP\""));

		var apiDocs = request("GET", "/v3/api-docs", null, null);
		assertEquals(200, apiDocs.statusCode());
		assertTrue(apiDocs.body().contains("\"bearerAuth\""));
		assertTrue(apiDocs.body().contains("/api/v1/manager/games"));

		var suffix = UUID.randomUUID().toString().substring(0, 8);
		var buyerEmail = "mysql-smoke-" + suffix + "@example.com";
		var buyerPassword = "smoke-buyer-password-123";
		assertEquals(201, request("POST", "/api/v1/auth/register", null,
				registration(buyerEmail, buyerPassword)).statusCode());
		var buyerToken = token(request("POST", "/api/v1/auth/login", null,
				credentials(buyerEmail, buyerPassword)));
		var managerToken = token(request("POST", "/api/v1/auth/login", null,
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword())));

		var sku = "MYSQL-" + suffix.toUpperCase();
		var gameCreation = request("POST", "/api/v1/manager/games", managerToken,
				"{\"sku\":\"" + sku + "\",\"title\":\"MySQL Smoke Game\",\"description\":\"CI smoke fixture\","
						+ "\"genre\":\"Strategy\",\"platform\":\"PC\",\"price\":19.99,\"stockQuantity\":2}");
		assertEquals(201, gameCreation.statusCode());
		assertTrue(gameCreation.headers().firstValue("Location").orElseThrow().startsWith("/api/v1/games/"));
		var gameId = id(gameCreation.body());

		var catalog = request("GET", "/api/v1/games?query=" + encode(sku)
				+ "&genre=Strategy&platform=PC&minimumPrice=10&maximumPrice=30&sort=price&direction=asc",
				buyerToken, null);
		assertEquals(200, catalog.statusCode());
		assertTrue(catalog.body().contains(sku));

		var purchase = request("POST", "/api/v1/purchases", buyerToken,
				"{\"items\":[{\"gameId\":" + gameId + ",\"quantity\":1}]}");
		assertEquals(201, purchase.statusCode());
		assertTrue(purchase.body().contains("\"totalAmount\":19.99"));
		assertTrue(purchase.body().contains("\"currency\":\"EUR\""));
		var purchaseId = id(purchase.body());

		var history = request("GET", "/api/v1/purchases/me", buyerToken, null);
		assertEquals(200, history.statusCode());
		assertTrue(history.body().contains("\"id\":" + purchaseId));

		var inventory = request("GET", "/api/v1/manager/inventory?query=" + encode(sku), managerToken, null);
		assertEquals(200, inventory.statusCode());
		assertTrue(inventory.body().contains("\"stockQuantity\":1"));
		assertTrue(inventory.body().contains("\"lowStock\":true"));

		var today = LocalDate.now();
		var statistics = request("GET", "/api/v1/manager/statistics/purchases?from=" + today + "&to=" + today,
				managerToken, null);
		assertEquals(200, statistics.statusCode());
		assertTrue(statistics.body().contains("\"totalOrders\":"));
		assertTrue(statistics.body().contains("\"totalRevenue\":"));
		assertTrue(statistics.body().contains("\"currency\":\"EUR\""));
	}

	private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
		var builder = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header("X-Correlation-ID", "mysql-smoke-" + UUID.randomUUID());
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
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

	private static String token(HttpResponse<String> response) {
		assertEquals(200, response.statusCode(), response.body());
		var matcher = ACCESS_TOKEN_PATTERN.matcher(response.body());
		assertTrue(matcher.find(), "response did not contain an access token: " + response.body());
		return matcher.group(1);
	}

	private static long id(String body) {
		var matcher = ID_PATTERN.matcher(body);
		assertTrue(matcher.find(), "response did not contain an ID: " + body);
		return Long.parseLong(matcher.group(1));
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String registration(String email, String password) {
		return "{\"email\":\"" + email + "\",\"displayName\":\"MySQL Smoke Buyer\",\"password\":\""
				+ password + "\"}";
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
