package com.gamestore.game_store_api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiErrorIntegrationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	@LocalServerPort
	private int port;

	@Test
	void returnsConsistentValidationProblemAndPreservesValidCorrelationId() throws Exception {
		var correlationId = "validation-test-123";
		var response = request("POST", "/api/auth/register", null, "{\"email\":\"not-an-email\",\"password\":\"short\"}",
				correlationId);

		assertEquals(400, response.statusCode());
		assertEquals(correlationId, response.headers().firstValue("X-Correlation-ID").orElseThrow());
		assertProblemContentType(response);
		assertContains(response.body(),
				"\"status\":400",
				"\"code\":\"validation_failed\"",
				"\"correlationId\":\"" + correlationId + "\"",
				"\"instance\":\"/api/auth/register\"",
				"\"timestamp\":",
				"\"errors\":",
				"\"field\":\"email\"",
				"\"field\":\"password\"");
	}

	@Test
	void usesSameProblemContractForAuthenticationAndAuthorizationFailures() throws Exception {
		var unauthorizedCorrelationId = "anonymous-request-123";
		var unauthorized = request("GET", "/api/catalog/games", null, null, unauthorizedCorrelationId);

		assertEquals(401, unauthorized.statusCode());
		assertEquals(unauthorizedCorrelationId,
				unauthorized.headers().firstValue("X-Correlation-ID").orElseThrow());
		assertProblemContentType(unauthorized);
		assertContains(unauthorized.body(),
				"\"code\":\"authentication_required\"",
				"\"correlationId\":\"" + unauthorizedCorrelationId + "\"");

		var buyerToken = registerAndLoginBuyer();
		var forbiddenCorrelationId = "forbidden-request-123";
		var forbidden = request("GET", "/api/manager/inventory", buyerToken, null, forbiddenCorrelationId);

		assertEquals(403, forbidden.statusCode());
		assertProblemContentType(forbidden);
		assertContains(forbidden.body(),
				"\"code\":\"access_denied\"",
				"\"correlationId\":\"" + forbiddenCorrelationId + "\"");
	}

	@Test
	void validatesQueryParametersAndReplacesUnsafeCorrelationIds() throws Exception {
		var buyerToken = registerAndLoginBuyer();
		var invalidCorrelationId = "contains spaces and must not be trusted";
		var response = request("GET", "/api/catalog/games?page=-1", buyerToken, null, invalidCorrelationId);

		assertEquals(400, response.statusCode());
		var generatedId = response.headers().firstValue("X-Correlation-ID").orElseThrow();
		assertNotEquals(invalidCorrelationId, generatedId);
		assertTrue(UUID_PATTERN.matcher(generatedId).matches());
		assertProblemContentType(response);
		assertContains(response.body(),
				"\"code\":\"validation_failed\"",
				"\"correlationId\":\"" + generatedId + "\"",
				"\"field\":\"page\"");
	}

	private String registerAndLoginBuyer() throws Exception {
		var email = "errors-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";
		assertEquals(201, request("POST", "/api/auth/register", null, credentials(email, password), null).statusCode());
		var login = request("POST", "/api/auth/login", null, credentials(email, password), null);
		assertEquals(200, login.statusCode());
		var matcher = ACCESS_TOKEN_PATTERN.matcher(login.body());
		assertTrue(matcher.find(), "response did not contain an access token");
		return matcher.group(1);
	}

	private HttpResponse<String> request(String method, String path, String accessToken, String body,
			String correlationId) throws Exception {
		var builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path));
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		if (correlationId != null) {
			builder.header("X-Correlation-ID", correlationId);
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

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static void assertProblemContentType(HttpResponse<String> response) {
		assertTrue(response.headers().firstValue("Content-Type").orElse("")
				.startsWith("application/problem+json"));
	}

	private static void assertContains(String body, String... fragments) {
		for (var fragment : fragments) {
			assertTrue(body.contains(fragment), () -> "response did not contain " + fragment + ": " + body);
		}
	}
}
