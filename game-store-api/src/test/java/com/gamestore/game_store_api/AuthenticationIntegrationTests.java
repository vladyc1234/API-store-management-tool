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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import com.gamestore.game_store_api.bootstrap.ManagerBootstrapService;
import com.gamestore.game_store_api.config.ManagerBootstrapProperties;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");

	@LocalServerPort
	private int port;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private ManagerBootstrapService managerBootstrapService;

	@Autowired
	private ManagerBootstrapProperties managerProperties;

	@Test
	void registersBuyerAndIssuesVerifiableJwtOnLogin() throws Exception {
		var email = "buyer-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";

		var registration = post("/api/auth/register", credentials(email, password));
		assertEquals(201, registration.statusCode());
		assertFalse(registration.body().contains(password));

		var savedBuyer = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertEquals(Role.BUYER, savedBuyer.getRole());
		assertNotEquals(password, savedBuyer.getPasswordHash());
		assertTrue(passwordEncoder.matches(password, savedBuyer.getPasswordHash()));

		var login = post("/api/auth/login", credentials(email, password));
		assertEquals(200, login.statusCode());

		var jwt = jwtDecoder.decode(accessToken(login.body()));
		assertEquals(email, jwt.getSubject());
		assertEquals(savedBuyer.getId().toString(), jwt.getClaim("userId").toString());
		assertEquals("BUYER", jwt.getClaimAsStringList("roles").getFirst());
	}

	@Test
	void rejectsDuplicateBuyerRegistrationAndInvalidLogin() throws Exception {
		var email = "duplicate-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";

		assertEquals(201, post("/api/auth/register", credentials(email, password)).statusCode());
		assertEquals(409, post("/api/auth/register", credentials(email, password)).statusCode());
		assertEquals(401, post("/api/auth/login", credentials(email, "wrong-password")).statusCode());
	}

	@Test
	void managerBootstrapIsIdempotentAndManagerCanLogin() throws Exception {
		var initialManager = userAccountRepository.findByEmailIgnoreCase(managerProperties.managerEmail()).orElseThrow();
		var initialId = initialManager.getId();

		managerBootstrapService.bootstrap();
		managerBootstrapService.bootstrap();

		var manager = userAccountRepository.findByEmailIgnoreCase(managerProperties.managerEmail()).orElseThrow();
		assertEquals(initialId, manager.getId());
		assertEquals(Role.MANAGER, manager.getRole());
		assertNotEquals(managerProperties.managerPassword(), manager.getPasswordHash());
		assertTrue(passwordEncoder.matches(managerProperties.managerPassword(), manager.getPasswordHash()));

		var login = post("/api/auth/login",
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword()));
		assertEquals(200, login.statusCode());
		assertEquals("MANAGER",
				jwtDecoder.decode(accessToken(login.body())).getClaimAsStringList("roles").getFirst());
	}

	private HttpResponse<String> post(String path, String body) throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String accessToken(String responseBody) {
		var matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain an access token");
		return matcher.group(1);
	}
}
