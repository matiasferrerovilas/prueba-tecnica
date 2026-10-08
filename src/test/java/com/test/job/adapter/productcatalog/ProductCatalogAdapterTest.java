package com.test.job.adapter.productcatalog;

import com.test.job.adapter.productcatalog.config.ProductCatalogClientConfig;
import com.test.job.adapter.productcatalog.config.ProductCatalogProperties;
import com.test.job.domain.Product;
import com.test.job.domain.exception.ProductNotFoundException;
import com.test.job.domain.exception.UpstreamServiceException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4jBulkheadProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCatalogAdapterTest {

	private static final Duration TEST_TIMEOUT = Duration.ofMillis(500);

	private MockWebServer mockWebServer;
	private ProductCatalogAdapter adapter;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		ProductCatalogClientConfig clientConfig = new ProductCatalogClientConfig();
		ProductCatalogProperties properties = new ProductCatalogProperties(
				"http://localhost:" + mockWebServer.getPort(), 1000, 0, 0);
		WebClient webClient = clientConfig.productCatalogWebClient(properties);
		ExistingProductApiClient apiClient = clientConfig.existingProductApiClient(webClient);

		ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory = new ReactiveResilience4JCircuitBreakerFactory(
				CircuitBreakerRegistry.ofDefaults(),
				TimeLimiterRegistry.ofDefaults(),
				new ReactiveResilience4jBulkheadProvider(BulkheadRegistry.ofDefaults()),
				new Resilience4JConfigurationProperties());
		circuitBreakerFactory.configureDefault(id -> new Resilience4JConfigBuilder(id)
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(TEST_TIMEOUT).build())
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				.build());

		adapter = new ProductCatalogAdapter(apiClient, circuitBreakerFactory);
		adapter.initCircuitBreakers();
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.close();
	}

	@Test
	void findsSimilarProductIds() {
		mockWebServer.enqueue(new MockResponse()
				.setBody("[\"2\",\"3\"]")
				.addHeader("Content-Type", "application/json"));

		StepVerifier.create(adapter.findSimilarProductIds("1"))
				.assertNext(ids -> assertThat(ids).containsExactly("2", "3"))
				.verifyComplete();
	}

	@Test
	void mapsProductDetailResponseToDomain() {
		mockWebServer.enqueue(new MockResponse()
				.setBody("{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}")
				.addHeader("Content-Type", "application/json"));

		StepVerifier.create(adapter.findProductDetail("2"))
				.expectNext(new Product("2", "Dress", 19.99, true))
				.verifyComplete();
	}

	@Test
	void mapsNotFoundResponseToDomainException() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(404));

		StepVerifier.create(adapter.findProductDetail("5"))
				.expectError(ProductNotFoundException.class)
				.verify();
	}

	@Test
	void mapsServerErrorToUpstreamServiceException() {
		mockWebServer.enqueue(new MockResponse().setResponseCode(500));

		StepVerifier.create(adapter.findProductDetail("6"))
				.expectError(UpstreamServiceException.class)
				.verify();
	}

	@Test
	void propagatesTimeoutAsIsWithoutWaitingForSlowResponse() {
		mockWebServer.enqueue(new MockResponse()
				.setBody("{\"id\":\"1000\",\"name\":\"Coat\",\"price\":89.99,\"availability\":true}")
				.addHeader("Content-Type", "application/json")
				.setBodyDelay(TEST_TIMEOUT.multipliedBy(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS));

		StepVerifier.create(adapter.findProductDetail("1000"))
				.expectError(java.util.concurrent.TimeoutException.class)
				.verify();
	}
}
