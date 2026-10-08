package com.test.job.application.service;

import com.test.job.application.port.ProductCatalogPort;
import com.test.job.domain.Product;
import com.test.job.domain.exception.ProductNotFoundException;
import com.test.job.domain.exception.UpstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilarProductsServiceTest {

	private ProductCatalogPort productCatalogPort;
	private SimilarProductsService service;

	@BeforeEach
	void setUp() {
		productCatalogPort = mock(ProductCatalogPort.class);
		service = new SimilarProductsService(productCatalogPort);
	}

	@Test
	void returnsSimilarProductsInOrder() {
		when(productCatalogPort.findSimilarProductIds("1")).thenReturn(Mono.just(List.of("2", "3")));
		when(productCatalogPort.findProductDetail("2"))
				.thenReturn(Mono.just(new Product("2", "Dress", 19.99, true)));
		when(productCatalogPort.findProductDetail("3"))
				.thenReturn(Mono.just(new Product("3", "Blazer", 29.99, false)));

		StepVerifier.create(service.getSimilarProducts("1"))
				.assertNext(result -> assertThat(result).extracting(Product::id).containsExactly("2", "3"))
				.verifyComplete();
	}

	@Test
	void skipsProductsThatFailToLoad() {
		when(productCatalogPort.findSimilarProductIds("4")).thenReturn(Mono.just(List.of("1", "2", "5")));
		when(productCatalogPort.findProductDetail("1"))
				.thenReturn(Mono.just(new Product("1", "Shirt", 9.99, true)));
		when(productCatalogPort.findProductDetail("2"))
				.thenReturn(Mono.just(new Product("2", "Dress", 19.99, true)));
		when(productCatalogPort.findProductDetail("5")).thenReturn(Mono.error(new ProductNotFoundException()));

		StepVerifier.create(service.getSimilarProducts("4"))
				.assertNext(result -> assertThat(result).extracting(Product::id).containsExactly("1", "2"))
				.verifyComplete();
	}

	@Test
	void skipsProductsThatTimeOutOrFailUpstream() {
		when(productCatalogPort.findSimilarProductIds("3")).thenReturn(Mono.just(List.of("100", "1000")));
		when(productCatalogPort.findProductDetail("100"))
				.thenReturn(Mono.just(new Product("100", "Trousers", 49.99, false)));
		when(productCatalogPort.findProductDetail("1000"))
				.thenReturn(Mono.error(new UpstreamServiceException("timed out", new RuntimeException())));

		StepVerifier.create(service.getSimilarProducts("3"))
				.assertNext(result -> assertThat(result).extracting(Product::id).containsExactly("100"))
				.verifyComplete();
	}

	@Test
	void propagatesNotFoundWhenMainProductIsUnknown() {
		when(productCatalogPort.findSimilarProductIds("999"))
				.thenReturn(Mono.error(new ProductNotFoundException()));

		StepVerifier.create(service.getSimilarProducts("999"))
				.expectError(ProductNotFoundException.class)
				.verify();
	}

	@Test
	void propagatesUpstreamFailuresFetchingSimilarIds() {
		when(productCatalogPort.findSimilarProductIds("5"))
				.thenReturn(Mono.error(new UpstreamServiceException("boom", new RuntimeException())));

		StepVerifier.create(service.getSimilarProducts("5"))
				.expectError(UpstreamServiceException.class)
				.verify();
	}
}
