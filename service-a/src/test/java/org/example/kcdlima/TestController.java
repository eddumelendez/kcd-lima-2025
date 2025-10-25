package org.example.kcdlima;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TestController {

	static List<CloudEvent<String>> events = new ArrayList<>();

	@PostMapping("/subscribe")
	@Topic(pubsubName = "pubsub", name = "notification")
	public void subscribe(@RequestBody CloudEvent<String> cloudEvent) {
		events.add(cloudEvent);
	}

}
