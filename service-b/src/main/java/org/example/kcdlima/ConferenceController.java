package org.example.kcdlima;

import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConferenceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConferenceController.class);

	private final DaprClient daprClient;

	public ConferenceController(DaprClient daprClient) {
		this.daprClient = daprClient;
	}

	@GetMapping("/conference")
	public String greetings() {
		var value = this.daprClient.getConfiguration("conferences", "devjvm").block().getValue();
		return value;
	}

	@GetMapping("/new-conference")
	public String newConference() {
		var value = this.daprClient.getConfiguration("conferences", "devjvm").block().getValue();
		return value.toUpperCase();
	}

	@PostMapping("/subscribe")
	@Topic(pubsubName = "pubsub", name = "notification")
	public void subscribe(@RequestBody CloudEvent<String> cloudEvent) {
		LOGGER.info("Received message from {} with data {}", cloudEvent.getSource(), cloudEvent.getData());
	}

}
