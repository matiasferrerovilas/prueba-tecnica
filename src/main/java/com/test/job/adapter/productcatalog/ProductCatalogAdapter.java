package com.test.job.adapter.productcatalog;

import com.test.job.adapter.productcatalog.config.ProductCatalogResilienceConfig;
import com.test.job.adapter.productcatalog.dto.ProductDetailResponse;
import com.test.job.application.port.ProductCatalogPort;
import com.test.job.domain.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductCatalogAdapter implements ProductCatalogPort {

	private final ExistingProductApiClient apiClient;
	private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

	private ReactiveCircuitBreaker similarIdsCircuitBreaker;
	private ReactiveCircuitBreaker productDetailCircuitBreaker;

	@PostConstruct
	void initCircuitBreakers() {
		similarIdsCircuitBreaker = circuitBreakerFactory.create(ProductCatalogResilienceConfig.SIMILAR_IDS);
		productDetailCircuitBreaker = circuitBreakerFactory.create(ProductCatalogResilienceConfig.PRODUCT_DETAIL);
	}

	@Override
	public Mono<List<String>> findSimilarProductIds(String productId) {
		return similarIdsCircuitBreaker.run(apiClient.getSimilarProductIds(productId), Mono::error);
	}

	@Override
	public Mono<Product> findProductDetail(String productId) {
		Mono<Product> call = apiClient.getProductDetail(productId).map(this::toDomain);
		return productDetailCircuitBreaker.run(call, Mono::error);
	}

	private Product toDomain(ProductDetailResponse response) {
		return new Product(response.id(), response.name(), response.price(), response.availability());
	}
}
