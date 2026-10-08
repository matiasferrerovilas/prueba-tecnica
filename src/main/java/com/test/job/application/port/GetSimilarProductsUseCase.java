package com.test.job.application.port;

import com.test.job.domain.Product;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetSimilarProductsUseCase {

	Mono<List<Product>> getSimilarProducts(String productId);
}
