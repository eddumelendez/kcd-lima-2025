package org.example.kcdlima;

import io.dapr.client.DaprClient;
import io.dapr.spring.boot.properties.pubsub.DaprPubSubProperties;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DaprPubSubProperties.class)
public class MessagingConfig {

	@Bean
	public DaprMessagingTemplate<String> messagingTemplate(DaprClient daprClient,
			DaprPubSubProperties daprPubSubProperties) {
		return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName(),
				daprPubSubProperties.isObservationEnabled());
	}

}
