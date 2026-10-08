package com.test.job.adapter.productcatalog.config;

import com.test.job.adapter.productcatalog.ExistingProductApiClient;
import com.test.job.domain.exception.ProductNotFoundException;
import com.test.job.domain.exception.UpstreamServiceException;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(ProductCatalogProperties.class)
public class ProductCatalogClientConfig {

	@Bean
	public WebClient productCatalogWebClient(ProductCatalogProperties properties) {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeoutMillis());

		return WebClient.builder()
				.baseUrl(properties.baseUrl())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.defaultStatusHandler(status -> status.value() == 404,
						response -> Mono.just(new ProductNotFoundException()))
				.defaultStatusHandler(HttpStatusCode::isError,
						response -> Mono.just(new UpstreamServiceException(
								"Existing product API responded with " + response.statusCode(), null)))
				.build();
	}

	@Bean
	public ExistingProductApiClient existingProductApiClient(WebClient productCatalogWebClient) {
		HttpServiceProxyFactory factory = HttpServiceProxyFactory
				.builderFor(WebClientAdapter.create(productCatalogWebClient))
				.build();
		return factory.createClient(ExistingProductApiClient.class);
	}
}
