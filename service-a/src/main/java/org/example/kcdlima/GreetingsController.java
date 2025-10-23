package org.example.kcdlima;

import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@RestController
public class GreetingsController {

	private final RestClient restClient;

	private final DaprMessagingTemplate<String> messagingTemplate;

	public GreetingsController(DaprConnectionDetails daprConnectionDetails, RestClient.Builder restClientBuilder,
			DaprMessagingTemplate<String> messagingTemplate) {
		this.restClient = restClientBuilder.baseUrl(daprConnectionDetails.getHttpEndpoint()).build();
		this.messagingTemplate = messagingTemplate;
	}

	@GetMapping("/greetings")
	public String greetings() {
		var name = this.restClient.get()
			.uri("/conference")
			.header("dapr-app-id", "service-b")
			.retrieve()
			.toEntity(String.class)
			.getBody();
		var now = LocalDateTime.now();
		this.messagingTemplate.send("notification", "Welcome message at " + now);
		return "Hello %s!!!".formatted(name);
	}

}
