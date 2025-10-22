package org.example.kcdlima;

import io.dapr.client.DaprClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConferenceController {
    
    private final DaprClient daprClient;

    public ConferenceController(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @GetMapping("/conference")
    public String greetings() {
        var value = this.daprClient.getConfiguration("kcdlima", "greeting")
                .block()
                .getValue();
        return value;
    }

}
