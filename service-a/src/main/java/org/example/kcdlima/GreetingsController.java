package org.example.kcdlima;

import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class GreetingsController {
    
    private final RestClient restClient;

    public GreetingsController(DaprConnectionDetails daprConnectionDetails, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(daprConnectionDetails.httpEndpoint() + "/v1.0/invoke").build();
    }

    @GetMapping("/greetings")
    public String greetings() {
        var name = this.restClient.get()
                .uri("/service-b/method/conference")
                .retrieve()
                .toEntity(String.class)
                .getBody();
        return "Hello %s!!!".formatted(name);
    }

}
