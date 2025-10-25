package org.example.kcdlima;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.Matchers.equalTo;

@Import(TestTestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KcdLima2025ApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
		Testcontainers.exposeHostPorts(port);

		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
		RestAssured.get("/greetings").then().statusCode(200).assertThat().body(equalTo("Hello DevJVM 2025!!!"));

		waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(TestController.events).hasSize(1));

		assertThat(TestController.events.get(0)).satisfies(event -> {
			assertThat(event.getSource()).isEqualTo("service-a");
			assertThat(event.getData()).startsWith("Welcome message at");
		});
	}

}
