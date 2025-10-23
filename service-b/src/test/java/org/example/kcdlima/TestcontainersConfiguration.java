package org.example.kcdlima;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.dapr.testcontainers.AppHttpPipeline;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.ListEntry;
import io.dapr.testcontainers.OtelTracingConfigurationSettings;
import io.dapr.testcontainers.TracingConfigurationSettings;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.grafana.LgtmStackContainer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    
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

            @Override
            public Statement apply(Statement base, Description description) {
                return null;
            }
        };

        List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
                .withNameFilter("dapr-network").exec();
        if (networks.isEmpty()) {
            Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
            return defaultDaprNetwork;
        } else {
            return defaultDaprNetwork;
        }
    }

    @Bean
    @ServiceConnection
    public DaprContainer daprContainer() {
        GenericContainer<?> redisContainer = new GenericContainer("redis:7-alpine") {
            @Override
            protected void containerIsStarted(InspectContainerResponse containerInfo) {
                try {
                    execInContainer("redis-cli", "-p", "6379", "MSET", "kcdlima", "KCD Lima 2025");
                    execInContainer("redis-cli", "-p", "6379", "MSET", "devjvm", "DevJVM 2025");
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Error running redis command", e);
                }
            }
        }
                .withNetwork(daprNetwork)
                .withNetworkAliases("redis")
                .withExposedPorts(6379)
                .withReuse(true);

        Map<String, String> redisMetadata = Map.of("redisHost", "redis:6379");
        
        LgtmStackContainer lgtmStackContainer = new LgtmStackContainer("grafana/otel-lgtm:0.11.4")
                .withReuse(true)
                .withNetwork(daprNetwork)
                .withNetworkAliases("lgtm-stack");

        var otel = new OtelTracingConfigurationSettings("lgtm-stack:4318", false, "http");
        var tracing = new TracingConfigurationSettings("1", true, otel, null);

        RabbitMQContainer rabbitMqContainer = new RabbitMQContainer("rabbitmq:3.9.11-alpine")
                .withNetwork(daprNetwork)
                .withNetworkAliases("rabbitmq")
                .withReuse(true);

        var pubsub = Map.of("connectionString", "amqp://guest:guest@rabbitmq:5672", "user", "guest", "password", "guest");

        List<ListEntry> handlers = List.of(new ListEntry("routeralias", "middleware.http.routeralias"));

        AppHttpPipeline appHttpPipeline = new AppHttpPipeline(handlers);
        
        var routes = """
                {
                    "/conference": "/new-conference"
                }
                """;
        var routesMetadata = Map.of("routes", routes);
        
        return new DaprContainer("daprio/daprd:1.16.0")
                .withAppName("service-b")
                .withAppChannelAddress("host.testcontainers.internal")
                .withAppPort(8081)
                .withNetwork(daprNetwork)
                .withReusablePlacement(true)
                .withReusableScheduler(true)
                .withConfiguration(new Configuration("otel-config", tracing, appHttpPipeline))
                .withComponent(new Component("conferences", "configuration.redis", "v1", redisMetadata))
                .withComponent(new Component("pubsub", "pubsub.rabbitmq", "v1", pubsub))
                .withComponent(new Component("routeralias", "middleware.http.routeralias", "v1", routesMetadata))
                .dependsOn(lgtmStackContainer, redisContainer, rabbitMqContainer);
    }

}
