package com.test.job;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
@org.springframework.test.context.TestPropertySource(properties = {
		"external-api.similar-ids-timeout-millis=2000",
		"external-api.product-detail-timeout-millis=1500"
})
class SimilarProductsIntegrationTest {

	private static final MockWebServer MOCK_SERVER = new MockWebServer();

	static {
		try {
			MOCK_SERVER.start();
			MOCK_SERVER.setDispatcher(new ExistingApiDispatcher());
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to start mock web server", e);
		}
	}

	@org.springframework.beans.factory.annotation.Autowired
	private WebTestClient webTestClient;

	@AfterAll
	static void stopMockServer() throws IOException {
		MOCK_SERVER.close();
	}

	@DynamicPropertySource
	static void registerMockServerUrl(DynamicPropertyRegistry registry) {
		registry.add("external-api.base-url", () -> "http://localhost:" + MOCK_SERVER.getPort());
	}

	private WebTestClient slowClient() {
		return webTestClient.mutate().responseTimeout(Duration.ofSeconds(10)).build();
	}

	@Test
	void returnsSimilarProductsOrderedBySimilarity() {
		ResponseSpec response = slowClient().get().uri("/product/1/similar").exchange();

		response.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[0].id").isEqualTo("2")
				.jsonPath("$[1].id").isEqualTo("3")
				.jsonPath("$[2].id").isEqualTo("4");
	}

	@Test
	void skipsSimilarProductsThatAreNotFound() {
		slowClient().get().uri("/product/4/similar")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].id").isEqualTo("1")
				.jsonPath("$[1].id").isEqualTo("2");
	}

	@Test
	void skipsSimilarProductsThatFailWithServerError() {
		slowClient().get().uri("/product/5/similar")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].id").isEqualTo("1")
				.jsonPath("$[1].id").isEqualTo("2");
	}

	@Test
	void skipsSimilarProductsThatTimeOutWithoutBlockingTheResponse() {
		slowClient().get().uri("/product/2/similar")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].id").isEqualTo("3")
				.jsonPath("$[1].id").isEqualTo("100");
	}

	@Test
	void returnsNotFoundWhenMainProductIsUnknown() {
		webTestClient.get().uri("/product/999/similar")
				.exchange()
				.expectStatus().isNotFound();
	}

	private static final class ExistingApiDispatcher extends Dispatcher {

		private static MockResponse json(int status, String body) {
			return new MockResponse()
					.setResponseCode(status)
					.setBody(body)
					.addHeader("Content-Type", "application/json");
		}

		@Override
		public MockResponse dispatch(RecordedRequest request) {
			String path = request.getPath();
			if ("/product/1000".equals(path)) {
				return json(200, "{\"id\":\"1000\",\"name\":\"Coat\",\"price\":89.99,\"availability\":true}")
						.setBodyDelay(5, java.util.concurrent.TimeUnit.SECONDS);
			}
			return switch (path) {
				case "/product/1/similarids" -> json(200, "[\"2\",\"3\",\"4\"]");
				case "/product/2/similarids" -> json(200, "[\"3\",\"100\",\"1000\"]");
				case "/product/4/similarids" -> json(200, "[\"1\",\"2\",\"5\"]");
				case "/product/5/similarids" -> json(200, "[\"1\",\"2\",\"6\"]");
				case "/product/999/similarids" -> new MockResponse().setResponseCode(404);
				case "/product/1" -> json(200, "{\"id\":\"1\",\"name\":\"Shirt\",\"price\":9.99,\"availability\":true}");
				case "/product/2" -> json(200, "{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}");
				case "/product/3" -> json(200, "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}");
				case "/product/4" -> json(200, "{\"id\":\"4\",\"name\":\"Boots\",\"price\":39.99,\"availability\":true}");
				case "/product/5" -> json(404, "{\"message\":\"Product not found\"}");
				case "/product/6" -> new MockResponse().setResponseCode(500);
				case "/product/100" -> json(200, "{\"id\":\"100\",\"name\":\"Trousers\",\"price\":49.99,\"availability\":false}");
				default -> new MockResponse().setResponseCode(404);
			};
		}
	}
}
