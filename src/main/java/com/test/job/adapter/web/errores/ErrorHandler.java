package com.test.job.adapter.web.errores;

import com.test.job.domain.exception.ProductNotFoundException;
import com.test.job.domain.exception.UpstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ProductNotFoundException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleNotFound(ProductNotFoundException ex) {
		log.info("Product not found: {}", ex.getMessage());

		var errorResponse = new ErrorResponse(
				String.valueOf(HttpStatus.NOT_FOUND.value()),
				"Not Found",
				ex.getMessage());

		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(errorResponse));
	}

	@ExceptionHandler(UpstreamServiceException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleUpstreamError(UpstreamServiceException ex) {
		log.error("Upstream service failure: {}", ex.getMessage(), ex);

		var errorResponse = new ErrorResponse(
				String.valueOf(HttpStatus.BAD_GATEWAY.value()),
				"Bad Gateway",
				ex.getMessage());

		return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(errorResponse));
	}

	@Override
	protected Mono<ResponseEntity<Object>> handleExceptionInternal(Exception ex, Object body,
			HttpHeaders headers, HttpStatusCode statusCode, ServerWebExchange exchange) {
		log.error("Unhandled exception", ex);

		var errorResponse = new ErrorResponse(
				String.valueOf(statusCode.value()),
				"Unexpected Error",
				ex.getMessage());

		return Mono.just(ResponseEntity.status(statusCode)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(errorResponse));
	}
}
