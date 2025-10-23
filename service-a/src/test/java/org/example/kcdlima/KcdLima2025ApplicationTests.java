package org.example.kcdlima;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KcdLima2025ApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
		RestAssured.registerParser("text/plain", Parser.TEXT);
		RestAssured.get("/greetings")
			.then()
			.statusCode(200)
			.assertThat()
			.body("message", equalTo("Hello KCD Lima 2025!!!"));
	}

}
