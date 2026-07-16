package com.gamestore.game_store_api.error;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.gamestore.game_store_api.auth.EmailAlreadyRegisteredException;
import com.gamestore.game_store_api.auth.InvalidPasswordException;
import com.gamestore.game_store_api.game.DuplicateGameSkuException;
import com.gamestore.game_store_api.game.GameConflictException;
import com.gamestore.game_store_api.game.GameNotFoundException;
import com.gamestore.game_store_api.game.InvalidStockAdjustmentException;
import com.gamestore.game_store_api.game.InvalidGameSearchException;
import com.gamestore.game_store_api.purchase.InvalidPurchaseRequestException;
import com.gamestore.game_store_api.purchase.InvalidStatisticsRangeException;
import com.gamestore.game_store_api.purchase.PurchaseAccessException;
import com.gamestore.game_store_api.purchase.PurchaseConflictException;
import com.gamestore.game_store_api.purchase.PurchaseGameNotFoundException;
import com.gamestore.game_store_api.purchase.PurchaseNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleRequestBodyValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		var errors = new ArrayList<ApiValidationError>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				errors.add(new ApiValidationError(error.getField(), error.getDefaultMessage())));
		exception.getBindingResult().getGlobalErrors().forEach(error ->
				errors.add(new ApiValidationError(error.getObjectName(), error.getDefaultMessage())));
		return ApiProblems.validation("One or more request body fields are invalid", errors, request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
		var errors = new ArrayList<ApiValidationError>();
		exception.getParameterValidationResults().forEach(result -> {
			var parameter = result.getMethodParameter();
			var field = parameter.getParameterName() != null
					? parameter.getParameterName()
					: "parameter[" + parameter.getParameterIndex() + "]";
			result.getResolvableErrors().forEach(error ->
					errors.add(new ApiValidationError(field, error.getDefaultMessage())));
		});
		return ApiProblems.validation("One or more request parameters are invalid", errors, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		var errors = exception.getConstraintViolations().stream()
				.map(violation -> new ApiValidationError(publicFieldName(violation.getPropertyPath().toString()),
						violation.getMessage()))
				.toList();
		return ApiProblems.validation("One or more request parameters are invalid", errors, request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception,
			HttpServletRequest request) {
		return ApiProblems.validation("A required request parameter is missing",
				java.util.List.of(new ApiValidationError(exception.getParameterName(), "must be provided")), request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		return ApiProblems.validation("A request parameter has an invalid type",
				java.util.List.of(new ApiValidationError(exception.getName(), "has an invalid value")), request);
	}

	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableMessage(HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.BAD_REQUEST, "malformed_request", "Malformed request",
				"The request body is missing or contains invalid JSON", request);
	}

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ProblemDetail handleDuplicateEmail(EmailAlreadyRegisteredException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "email_already_registered", "Email already registered", exception,
				request);
	}

	@ExceptionHandler(InvalidPasswordException.class)
	ProblemDetail handleInvalidPassword(InvalidPasswordException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "invalid_password", "Invalid password", exception, request);
	}

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Authentication failed", exception, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleMethodAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.FORBIDDEN, "access_denied", "Access denied",
				"Your account does not have permission to access this resource", request);
	}

	@ExceptionHandler(GameNotFoundException.class)
	ProblemDetail handleGameNotFound(GameNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "game_not_found", "Game not found", exception, request);
	}

	@ExceptionHandler(DuplicateGameSkuException.class)
	ProblemDetail handleDuplicateSku(DuplicateGameSkuException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "duplicate_game_sku", "Duplicate game SKU", exception, request);
	}

	@ExceptionHandler(InvalidStockAdjustmentException.class)
	ProblemDetail handleInvalidStockAdjustment(InvalidStockAdjustmentException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "invalid_stock_adjustment", "Invalid stock adjustment", exception,
				request);
	}

	@ExceptionHandler(InvalidGameSearchException.class)
	ProblemDetail handleInvalidGameSearch(InvalidGameSearchException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "invalid_game_search", "Invalid game search", exception, request);
	}

	@ExceptionHandler(GameConflictException.class)
	ProblemDetail handleGameConflict(GameConflictException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "game_update_conflict", "Game update conflict", exception, request);
	}

	@ExceptionHandler({PurchaseNotFoundException.class, PurchaseGameNotFoundException.class})
	ProblemDetail handlePurchaseResourceNotFound(RuntimeException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "purchase_resource_not_found", "Purchase resource not found", exception,
				request);
	}

	@ExceptionHandler(InvalidPurchaseRequestException.class)
	ProblemDetail handleInvalidPurchase(InvalidPurchaseRequestException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "invalid_purchase_request", "Invalid purchase request", exception,
				request);
	}

	@ExceptionHandler(PurchaseAccessException.class)
	ProblemDetail handlePurchaseAccess(PurchaseAccessException exception, HttpServletRequest request) {
		return problem(HttpStatus.FORBIDDEN, "purchase_access_denied", "Purchase access denied", exception, request);
	}

	@ExceptionHandler(PurchaseConflictException.class)
	ProblemDetail handlePurchaseConflict(PurchaseConflictException exception, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "purchase_conflict", "Purchase conflict", exception, request);
	}

	@ExceptionHandler(InvalidStatisticsRangeException.class)
	ProblemDetail handleInvalidStatisticsRange(InvalidStatisticsRangeException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "invalid_statistics_range", "Invalid statistics date range",
				exception, request);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.CONFLICT, "concurrent_update", "Concurrent update",
				"The resource changed while this request was being processed; retry with fresh data", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ProblemDetail handleNoResource(HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.NOT_FOUND, "resource_not_found", "Resource not found",
				"No API resource exists at the requested path", request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ProblemDetail handleMethodNotSupported(HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "Method not allowed",
				"The HTTP method is not supported for this resource", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ProblemDetail handleMediaTypeNotSupported(HttpServletRequest request) {
		return ApiProblems.create(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type",
				"Unsupported media type", "The request Content-Type is not supported", request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled API exception", exception);
		return ApiProblems.create(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Internal server error",
				"An unexpected error occurred", request);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String title, RuntimeException exception,
			HttpServletRequest request) {
		log.debug("Returning API error {}: {}", code, exception.getMessage());
		return ApiProblems.create(status, code, title, exception.getMessage(), request);
	}

	private static String publicFieldName(String propertyPath) {
		var separator = propertyPath.lastIndexOf('.');
		return separator >= 0 ? propertyPath.substring(separator + 1) : propertyPath;
	}
}
