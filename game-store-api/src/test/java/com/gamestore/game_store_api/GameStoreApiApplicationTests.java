package com.gamestore.game_store_api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import com.gamestore.game_store_api.game.GameRepository;
import com.gamestore.game_store_api.purchase.PurchaseRepository;
import com.gamestore.game_store_api.user.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GameStoreApiApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void applicationStartsWithCoreInfrastructure() {
		assertEquals("1", applicationContext.getBean(Flyway.class).info().current().getVersion().toString());
		assertTrue(applicationContext.getBeanNamesForType(SecurityFilterChain.class).length > 0);
		assertTrue(applicationContext.getBeanNamesForType(UserAccountRepository.class).length > 0);
		assertTrue(applicationContext.getBeanNamesForType(GameRepository.class).length > 0);
		assertTrue(applicationContext.getBeanNamesForType(PurchaseRepository.class).length > 0);
	}

	@Test
	void healthEndpointIsPublicAndReportsUp() throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/actuator/health"))
				.GET()
				.build();

		var response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"status\":\"UP\""));
		assertTrue(response.headers().firstValue("X-Correlation-ID").isPresent());
	}

	@Test
	void openApiDescriptionIsPublicAndDocumentsBearerSecurity() throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/v3/api-docs"))
				.GET()
				.build();

		var response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"title\":\"Game Store Management API\""));
		assertTrue(response.body().contains("\"bearerAuth\""));
		assertTrue(response.body().contains("/api/buyer/purchases"));
		assertTrue(response.body().contains("/api/manager/statistics/purchases"));
	}
}
