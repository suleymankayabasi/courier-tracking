package com.migrosone.couriertracking.exception;

import com.migrosone.couriertracking.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(CourierNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleCourierNotFound(CourierNotFoundException ex, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(InvalidLocationTimeException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidLocationTime(
			InvalidLocationTimeException ex, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleConcurrencyFailure(
			ConcurrencyFailureException ex, HttpServletRequest request) {
		log.warn("Giving up after repeated concurrent updates: {}", ex.getMessage());
		return respond(HttpStatus.CONFLICT, "Concurrent update of the same courier, please retry", request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {}", request.getRequestURI(), ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ApiErrorResponse body = body(status, "Request validation failed", path(request), fieldErrors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ApiErrorResponse body = body(status, "Malformed request body", path(request), null);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		Object errorBody = body instanceof ApiErrorResponse
				? body
				: body(statusCode, messageOf(ex, statusCode), path(request), null);
		return super.handleExceptionInternal(ex, errorBody, headers, statusCode, request);
	}

	private static ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String message, String path) {
		return ResponseEntity.status(status).body(body(status, message, path, null));
	}

	private static ApiErrorResponse body(
			HttpStatusCode status, String message, String path, Map<String, String> fieldErrors) {
		return new ApiErrorResponse(Instant.now(), status.value(), reasonPhrase(status), message, path, fieldErrors);
	}

	private static String messageOf(Exception ex, HttpStatusCode status) {
		if (ex instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
			return errorResponse.getBody().getDetail();
		}
		return reasonPhrase(status);
	}

	private static String reasonPhrase(HttpStatusCode status) {
		HttpStatus resolved = HttpStatus.resolve(status.value());
		return resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
	}

	private static String path(WebRequest request) {
		return request instanceof ServletWebRequest servletRequest ? servletRequest.getRequest().getRequestURI() : null;
	}
}
