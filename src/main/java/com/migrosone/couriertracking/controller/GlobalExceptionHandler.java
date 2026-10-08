package com.migrosone.couriertracking.controller;

import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(CourierNotFoundException.class)
	ProblemDetail handleCourierNotFound(CourierNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidLocationException.class)
	ProblemDetail handleInvalidLocation(InvalidLocationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new TreeMap<>();
		ex.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Invalid location");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
				"Malformed request body (time must be ISO-8601, e.g. 2026-01-01T10:00:00Z)");
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
	}
}
