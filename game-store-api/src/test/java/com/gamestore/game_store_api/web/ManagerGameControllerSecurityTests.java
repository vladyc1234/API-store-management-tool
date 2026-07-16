package com.gamestore.game_store_api.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gamestore.game_store_api.config.JwtProperties;
import com.gamestore.game_store_api.config.SecurityConfiguration;
import com.gamestore.game_store_api.error.ApiAccessDeniedHandler;
import com.gamestore.game_store_api.error.ApiAuthenticationEntryPoint;
import com.gamestore.game_store_api.error.CorrelationIdFilter;
import com.gamestore.game_store_api.error.GlobalApiExceptionHandler;
import com.gamestore.game_store_api.error.SecurityProblemWriter;
import com.gamestore.game_store_api.game.GameResponse;
import com.gamestore.game_store_api.game.GameService;
import com.gamestore.game_store_api.game.ManagerGameController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = ManagerGameController.class)
@EnableConfigurationProperties(JwtProperties.class)
@Import({
		SecurityConfiguration.class,
		GlobalApiExceptionHandler.class,
		CorrelationIdFilter.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class ManagerGameControllerSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameService gameService;

	@Test
	void managerCanCreateGame() throws Exception {
		var now = LocalDateTime.of(2026, 1, 1, 12, 0);
		when(gameService.create(any())).thenReturn(new GameResponse(
				12L, "WEB-1", "Web Test", null, new BigDecimal("29.99"), 5, true, 0, now, now));

		mockMvc.perform(post("/api/manager/games")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
				.header("X-Correlation-ID", "controller-test-123")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sku":"WEB-1","title":"Web Test","description":null,"price":29.99,"stockQuantity":5}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("X-Correlation-ID", "controller-test-123"))
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.sku").value("WEB-1"))
				.andExpect(jsonPath("$.price").value(29.99));

		verify(gameService).create(any());
	}

	@Test
	void rejectsAnonymousAndBuyerRequestsWithConsistentSecurityProblems() throws Exception {
		var body = """
				{"sku":"SEC-1","title":"Security Test","price":10.00,"stockQuantity":1}
				""";

		mockMvc.perform(post("/api/manager/games")
				.header("X-Correlation-ID", "anonymous-controller-123")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("authentication_required"))
				.andExpect(jsonPath("$.correlationId").value("anonymous-controller-123"));

		mockMvc.perform(post("/api/manager/games")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BUYER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));

		verify(gameService, never()).create(any());
	}

	@Test
	void validatesManagerRequestBeforeCallingService() throws Exception {
		mockMvc.perform(post("/api/manager/games")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sku":"VALIDATION-1","title":"Invalid Price","price":-1.00,"stockQuantity":1}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.errors[0].field").value("price"));

		verify(gameService, never()).create(any());
	}
}
