package com.migrosone.couriertracking.exception;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps errors to RFC 9457 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CourierNotFoundException.class)
	ProblemDetail handleCourierNotFound(CourierNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidLocationException.class)
	ProblemDetail handleInvalidLocation(InvalidLocationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new TreeMap<>();
		ex.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid location");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadable() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Malformed request body (time must be ISO-8601, e.g. 2026-01-01T10:00:00Z)");
	}
}
