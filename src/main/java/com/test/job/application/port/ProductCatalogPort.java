package com.test.job.application.port;

import com.test.job.domain.Product;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductCatalogPort {

	Mono<List<String>> findSimilarProductIds(String productId);

	Mono<Product> findProductDetail(String productId);
}
