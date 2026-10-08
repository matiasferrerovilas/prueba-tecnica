package com.test.job.adapter.productcatalog.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProductCatalogResilienceConfig {

	public static final String SIMILAR_IDS = "similarProductIds";
	public static final String PRODUCT_DETAIL = "productDetail";

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerCustomizer(
			ProductCatalogProperties properties) {
		return factory -> {
			factory.configure(builder -> builder
							.timeLimiterConfig(TimeLimiterConfig.custom()
									.timeoutDuration(Duration.ofMillis(properties.similarIdsTimeoutMillis()))
									.build())
							.circuitBreakerConfig(CircuitBreakerConfig.custom()
									.slidingWindowSize(20)
									.minimumNumberOfCalls(10)
									.failureRateThreshold(50)
									.waitDurationInOpenState(Duration.ofSeconds(5))
									.permittedNumberOfCallsInHalfOpenState(5)
									.build()),
					SIMILAR_IDS);

			factory.configure(builder -> builder
							.timeLimiterConfig(TimeLimiterConfig.custom()
									.timeoutDuration(Duration.ofMillis(properties.productDetailTimeoutMillis()))
									.build())
							.circuitBreakerConfig(CircuitBreakerConfig.custom()
									.slidingWindowSize(50)
									.minimumNumberOfCalls(20)
									.failureRateThreshold(50)
									.waitDurationInOpenState(Duration.ofSeconds(5))
									.permittedNumberOfCallsInHalfOpenState(10)
									.build()),
					PRODUCT_DETAIL);

			factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
					.timeLimiterConfig(TimeLimiterConfig.ofDefaults())
					.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
					.build());
		};
	}
}
