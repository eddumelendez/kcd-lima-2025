package org.example.kcdlima;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.grafana.LgtmStackContainer;

import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger("wiremock");

	private static final Network daprNetwork = createNetwork();

	static Network createNetwork() {
		Network defaultDaprNetwork = new Network() {
			@Override
			public String getId() {
				return "dapr-network";
			}

			@Override
			public void close() {

			}
		};

		List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance()
			.client()
			.listNetworksCmd()
			.withNameFilter("dapr-network")
			.exec();
		if (networks.isEmpty()) {
			Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
			return defaultDaprNetwork;
		}
		else {
			return defaultDaprNetwork;
		}
	}

	@Bean
	@ServiceConnection
	public DaprContainer daprContainer() {
		LgtmStackContainer lgtmStackContainer = new LgtmStackContainer("grafana/otel-lgtm:0.21.0")
			.withNetwork(daprNetwork)
			.withNetworkAliases("lgtm-stack")
			.withReuse(true);

		var otel = new OtelTracingConfigurationSettings("lgtm-stack:4318", false, "http");
		var tracing = new TracingConfigurationSettings("1", true, otel, null);

		RabbitMQContainer rabbitMqContainer = new RabbitMQContainer("rabbitmq:3.9.11-alpine").withNetwork(daprNetwork)
			.withNetworkAliases("rabbitmq")
			.withReuse(true);

		var pubsub = Map.of("connectionString", "amqp://guest:guest@rabbitmq:5672", "user", "guest", "password",
				"guest");

		return new DaprContainer("daprio/daprd:1.17.0").withAppName("service-a")
			.withAppPort(8080)
			.withNetwork(daprNetwork)
			.withReusablePlacement(true)
			.withReusableScheduler(true)
			.withConfiguration(new Configuration("config", tracing, null))
			.withComponent(new Component("pubsub", "pubsub.rabbitmq", "v1", pubsub))
			.dependsOn(lgtmStackContainer, rabbitMqContainer);
	}

}
