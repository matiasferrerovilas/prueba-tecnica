package com.test.job.adapter.web.controllers;

import com.test.job.application.port.GetSimilarProductsUseCase;
import com.test.job.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SimilarProductsController {

	private final GetSimilarProductsUseCase getSimilarProductsUseCase;

	@GetMapping("/product/{productId}/similar")
	public Mono<List<Product>> getSimilarProducts(@PathVariable String productId) {
		return getSimilarProductsUseCase.getSimilarProducts(productId);
	}
}
