package com.test.job.application.service;

import com.test.job.application.port.in.GetSimilarProductsUseCase;
import com.test.job.application.port.out.ProductCatalogPort;
import com.test.job.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimilarProductsService implements GetSimilarProductsUseCase {

	private static final int DETAIL_FETCH_CONCURRENCY = 8;

	private final ProductCatalogPort productCatalogPort;

	@Override
	public Mono<List<Product>> getSimilarProducts(String productId) {
		return productCatalogPort.findSimilarProductIds(productId)
				.flatMapMany(Flux::fromIterable)
				.flatMapSequential(this::fetchProductOrSkip, DETAIL_FETCH_CONCURRENCY)
				.collectList();
	}

	private Mono<Product> fetchProductOrSkip(String similarProductId) {
		return productCatalogPort.findProductDetail(similarProductId)
				.onErrorResume(throwable -> Mono.empty());
	}
}
