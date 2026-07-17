package com.gamestore.game_store_api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
@Import(AuthenticationIntegrationTests.SecurityProbeConfiguration.class)
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

		var registration = post("/api/v1/auth/register", "{\"email\":\"" + email
				+ "\",\"displayName\":\"JWT Buyer\",\"password\":\"" + password + "\"}");
		assertEquals(201, registration.statusCode());
		assertFalse(registration.body().contains(password));

		var savedBuyer = userAccountRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertEquals(Role.BUYER, savedBuyer.getRole());
		assertEquals("JWT Buyer", savedBuyer.getDisplayName());
		assertNotEquals(password, savedBuyer.getPasswordHash());
		assertTrue(passwordEncoder.matches(password, savedBuyer.getPasswordHash()));

		var login = post("/api/v1/auth/login", credentials(email, password));
		assertEquals(200, login.statusCode());
		assertTrue(login.body().contains("\"expiresIn\":3600"));

		var jwt = jwtDecoder.decode(accessToken(login.body()));
		assertEquals(email, jwt.getSubject());
		assertEquals(savedBuyer.getId().toString(), jwt.getClaim("userId").toString());
		assertEquals("BUYER", jwt.getClaimAsStringList("roles").getFirst());
		assertEquals("game-store-api-clients", jwt.getAudience().getFirst());
	}

	@Test
	void registrationAcceptsSixCharactersAndRequiresLettersAndSpecialCharacters() throws Exception {
		var validEmail = "six-character-" + UUID.randomUUID() + "@example.com";
		assertEquals(201, post("/api/v1/auth/register", credentials(validEmail, "Abc1!2")).statusCode());

		var maximumLengthEmail = "maximum-length-" + UUID.randomUUID() + "@example.com";
		assertEquals(201, post("/api/v1/auth/register",
				credentials(maximumLengthEmail, "A!" + "1".repeat(70))).statusCode());

		var tooShortEmail = "too-short-" + UUID.randomUUID() + "@example.com";
		assertEquals(400, post("/api/v1/auth/register", credentials(tooShortEmail, "Ab!12")).statusCode());

		var tooLongEmail = "too-long-" + UUID.randomUUID() + "@example.com";
		assertEquals(400, post("/api/v1/auth/register",
				credentials(tooLongEmail, "A!" + "1".repeat(71))).statusCode());

		var noSpecialCharacterEmail = "no-special-" + UUID.randomUUID() + "@example.com";
		assertEquals(400,
				post("/api/v1/auth/register", credentials(noSpecialCharacterEmail, "Abc123")).statusCode());

		var noLetterEmail = "no-letter-" + UUID.randomUUID() + "@example.com";
		assertEquals(400,
				post("/api/v1/auth/register", credentials(noLetterEmail, "12345!")).statusCode());
	}

	@Test
	void unversionedAuthenticationAliasIsNotExposed() throws Exception {
		var email = "versioned-auth-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";
		assertEquals(201, post("/api/v1/auth/register", credentials(email, password)).statusCode());
		var login = post("/api/v1/auth/login", credentials(email, password));
		var token = accessToken(login.body());

		assertEquals(404, post("/api/auth/login", credentials(email, password), token).statusCode());
	}

	@Test
	void rejectsDuplicateBuyerRegistrationAndInvalidLogin() throws Exception {
		var email = "duplicate-" + UUID.randomUUID() + "@example.com";
		var password = "safe-password-123";

		assertEquals(201, post("/api/v1/auth/register", credentials(email, password)).statusCode());
		assertEquals(409, post("/api/v1/auth/register", credentials(email, password)).statusCode());
		assertEquals(401, post("/api/v1/auth/login", credentials(email, "wrong-password")).statusCode());
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

		var login = post("/api/v1/auth/login",
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword()));
		assertEquals(200, login.statusCode());
		assertEquals("MANAGER",
				jwtDecoder.decode(accessToken(login.body())).getClaimAsStringList("roles").getFirst());
	}

	@Test
	void enforcesStatelessBuyerManagerAndMethodRoleMappings() throws Exception {
		var buyerEmail = "roles-" + UUID.randomUUID() + "@example.com";
		var buyerPassword = "safe-password-123";
		assertEquals(201, post("/api/v1/auth/register", credentials(buyerEmail, buyerPassword)).statusCode());

		var buyerToken = accessToken(post("/api/v1/auth/login", credentials(buyerEmail, buyerPassword)).body());
		var managerToken = accessToken(post("/api/v1/auth/login",
				credentials(managerProperties.managerEmail(), managerProperties.managerPassword())).body());

		var anonymousResponse = get("/api/v1/purchases/security-probe", null);
		assertEquals(401, anonymousResponse.statusCode());
		assertTrue(anonymousResponse.headers().firstValue("Set-Cookie").isEmpty());

		assertEquals(200, get("/api/v1/purchases/security-probe", buyerToken).statusCode());
		assertEquals(403, get("/api/v1/purchases/security-probe", managerToken).statusCode());
		assertEquals(403, get("/api/manager/security-probe", buyerToken).statusCode());
		assertEquals(200, get("/api/manager/security-probe", managerToken).statusCode());
		assertEquals(403, get("/api/security/method-manager-probe", buyerToken).statusCode());
		assertEquals(200, get("/api/security/method-manager-probe", managerToken).statusCode());
	}

	private HttpResponse<String> post(String path, String body) throws Exception {
		return post(path, body, null);
	}

	private HttpResponse<String> post(String path, String body, String accessToken) throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body));
		if (accessToken != null) {
			request.header("Authorization", "Bearer " + accessToken);
		}
		return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path, String accessToken) throws Exception {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.GET();
		if (accessToken != null) {
			request.header("Authorization", "Bearer " + accessToken);
		}
		return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private static String accessToken(String responseBody) {
		var matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
		assertTrue(matcher.find(), "response did not contain an access token");
		return matcher.group(1);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SecurityProbeConfiguration {

		@Bean
		SecurityProbeController securityProbeController() {
			return new SecurityProbeController();
		}
	}

	@RestController
	static class SecurityProbeController {

		@GetMapping("/api/v1/purchases/security-probe")
		String buyerProbe() {
			return "buyer";
		}

		@GetMapping("/api/manager/security-probe")
		String managerProbe() {
			return "manager";
		}

		@PreAuthorize("hasRole('MANAGER')")
		@GetMapping("/api/security/method-manager-probe")
		String methodManagerProbe() {
			return "manager";
		}
	}
}

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthenticationServiceUnitTests {

	@org.mockito.Mock
	private com.gamestore.game_store_api.user.UserAccountRepository userRepository;
	@org.mockito.Mock
	private com.gamestore.game_store_api.auth.JwtTokenService jwtTokenService;

	@Test
	void registrationNormalizesEmailAndUsesBcrypt() {
		var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
		var service = new com.gamestore.game_store_api.auth.AuthenticationService(
				userRepository, encoder, jwtTokenService);
		org.mockito.Mockito.when(userRepository.existsByEmailIgnoreCase(" Buyer@Example.com ")).thenReturn(false);
		org.mockito.Mockito.when(userRepository.saveAndFlush(
				org.mockito.ArgumentMatchers.any(com.gamestore.game_store_api.user.UserAccount.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var account = service.registerBuyer(new com.gamestore.game_store_api.auth.RegisterRequest(
				" Buyer@Example.com ", "Player One", "safe-password-123"));

		org.assertj.core.api.Assertions.assertThat(account.getEmail()).isEqualTo("buyer@example.com");
		org.assertj.core.api.Assertions.assertThat(account.getDisplayName()).isEqualTo("Player One");
		org.assertj.core.api.Assertions.assertThat(encoder.matches("safe-password-123", account.getPasswordHash()))
				.isTrue();
	}

	@Test
	void duplicateRegistrationAndUnknownLoginFailSafely() {
		var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
		var service = new com.gamestore.game_store_api.auth.AuthenticationService(
				userRepository, encoder, jwtTokenService);
		org.mockito.Mockito.when(userRepository.existsByEmailIgnoreCase("buyer@example.com")).thenReturn(true);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.registerBuyer(
				new com.gamestore.game_store_api.auth.RegisterRequest(
						"buyer@example.com", "Buyer", "safe-password-123")))
				.isInstanceOf(com.gamestore.game_store_api.auth.EmailAlreadyRegisteredException.class);

		org.mockito.Mockito.when(userRepository.findByEmailIgnoreCase("missing@example.com"))
				.thenReturn(java.util.Optional.empty());
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.login(
				new com.gamestore.game_store_api.auth.LoginRequest("missing@example.com", "safe-password-123")))
				.isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
				.hasMessage("Invalid email or password");
	}
}
