package com.test.job.adapter.productcatalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api")
public record ProductCatalogProperties(
		String baseUrl,
		long connectTimeoutMillis,
		long similarIdsTimeoutMillis,
		long productDetailTimeoutMillis) {
}
