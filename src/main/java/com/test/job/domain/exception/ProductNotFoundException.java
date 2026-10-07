package com.test.job.domain.exception;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException() {
		super("Product not found");
	}
}
