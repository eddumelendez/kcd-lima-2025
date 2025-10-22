package org.example.kcdlima;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
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
                    execInContainer("redis-cli", "-p", "6379", "MSET", "greeting", "KCD Lima 2025");
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
        
        return new DaprContainer("daprio/daprd:1.15.4")
                .withAppName("service-b")
                .withAppChannelAddress("host.testcontainers.internal")
                .withAppPort(8081)
                .withNetwork(daprNetwork)
                .withReusablePlacement(true)
                .withConfiguration(new Configuration("otel-config", tracing))
                .withComponent(new Component("kcdlima", "configuration.redis", "v1", redisMetadata))
                .dependsOn(lgtmStackContainer, redisContainer);
    }

}
