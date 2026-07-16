package com.gamestore.game_store_api.error;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-ID";
	public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";
	public static final String MDC_KEY = "correlationId";

	private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
	private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		var requestedId = request.getHeader(HEADER_NAME);
		var correlationId = isValid(requestedId) ? requestedId : UUID.randomUUID().toString();
		var startedAt = System.nanoTime();

		request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
		response.setHeader(HEADER_NAME, correlationId);

		try (var ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
			log.debug("Processing HTTP {} {} with {} correlation ID", request.getMethod(), request.getRequestURI(),
					requestedId != null && requestedId.equals(correlationId) ? "client-supplied" : "generated");
			filterChain.doFilter(request, response);
		}
		finally {
			var elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
			try (var ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
				log.info("HTTP {} {} completed with status {} in {} ms", request.getMethod(), request.getRequestURI(),
						response.getStatus(), elapsedMillis);
			}
		}
	}

	private static boolean isValid(String value) {
		return value != null && VALID_CORRELATION_ID.matcher(value).matches();
	}
}
