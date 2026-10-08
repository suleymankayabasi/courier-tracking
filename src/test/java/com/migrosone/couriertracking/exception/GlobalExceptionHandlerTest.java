package com.migrosone.couriertracking.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.migrosone.couriertracking.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {
	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void errorResponseWithoutDetailFallsBackToTheReasonPhrase() {
		ApiErrorResponse body = handle(new ErrorResponseException(HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST,
				servletRequest("/x"));

		assertThat(body.message()).isEqualTo("Bad Request");
		assertThat(body.path()).isEqualTo("/x");
	}

	@Test
	void exceptionWithoutProblemDetailFallsBackToTheReasonPhrase() {
		ApiErrorResponse body = handle(new IllegalArgumentException("internal"), HttpStatus.BAD_REQUEST,
				servletRequest("/x"));

		assertThat(body.message()).isEqualTo("Bad Request");
	}

	@Test
	void unknownStatusCodeIsReportedAsItsNumber() {
		ApiErrorResponse body = handle(new IllegalArgumentException("internal"), HttpStatusCode.valueOf(499),
				servletRequest("/x"));

		assertThat(body.status()).isEqualTo(499);
		assertThat(body.error()).isEqualTo("499");
	}

	@Test
	void requestWithoutServletPathHasNoPath() {
		ApiErrorResponse body = handle(new IllegalArgumentException("internal"), HttpStatus.BAD_REQUEST,
				mock(WebRequest.class));

		assertThat(body.path()).isNull();
	}

	private ApiErrorResponse handle(Exception ex, HttpStatusCode status, WebRequest request) {
		ResponseEntity<Object> response = handler.handleExceptionInternal(ex, null, new HttpHeaders(), status, request);
		return (ApiErrorResponse) response.getBody();
	}

	private static WebRequest servletRequest(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		return new ServletWebRequest(request);
	}
}
