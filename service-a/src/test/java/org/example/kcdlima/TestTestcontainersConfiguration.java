package org.example.kcdlima;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprPlacementContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
class TestTestcontainersConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger("wiremock");

	private static final Network daprNetwork = Network.newNetwork();

	static final String JSON = """
			{
			  "request": {
			    "method": "GET",
			    "url": "/conference"
			  },
			  "response": {
			    "status": 200,
			    "body": "DevJVM 2025",
			    "headers": {
			      "Content-Type": "text/plain"
			    }
			  }
			}
			""";

	@Bean
	@ServiceConnection
	public DaprContainer daprContainer() {
		DaprPlacementContainer placementContainer = new DaprPlacementContainer("daprio/placement")
			.withNetwork(daprNetwork)
			.withNetworkAliases("placement");

		WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:3.13.1-alpine").withMappingFromJSON(JSON)
			.withLogConsumer(new Slf4jLogConsumer(LOGGER))
			.withNetwork(daprNetwork)
			.withNetworkAliases("wiremock");

		DaprContainer daprContainer = new DaprContainer("daprio/daprd:1.16.0").withAppName("service-b")
			.withAppChannelAddress("wiremock")
			.withAppPort(8080)
			.withNetwork(daprNetwork)
			.withPlacementContainer(placementContainer)
			.withAppHealthCheckPath("/__admin/health")
			.dependsOn(wireMock, placementContainer);

		RabbitMQContainer rabbitMqContainer = new RabbitMQContainer("rabbitmq:3.9.11-alpine").withNetwork(daprNetwork)
			.withNetworkAliases("rabbitmq");

		var pubsub = Map.of("connectionString", "amqp://guest:guest@rabbitmq:5672", "user", "guest", "password",
				"guest");

		return new DaprContainer("daprio/daprd:1.16.0").withAppName("service-a")
			.withAppPort(8080)
			.withAppChannelAddress("host.testcontainers.internal")
			.withNetwork(daprNetwork)
			.withPlacementContainer(placementContainer)
			.withComponent(new Component("pubsub", "pubsub.rabbitmq", "v1", pubsub))
			.dependsOn(rabbitMqContainer, daprContainer);
	}

}
