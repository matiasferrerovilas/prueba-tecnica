package com.test.job.adapter.productcatalog;

import com.test.job.adapter.productcatalog.dto.ProductDetailResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ExistingProductApiClient {

	@GetExchange("/product/{productId}/similarids")
	Mono<List<String>> getSimilarProductIds(@PathVariable String productId);

	@GetExchange("/product/{productId}")
	Mono<ProductDetailResponse> getProductDetail(@PathVariable String productId);
}
