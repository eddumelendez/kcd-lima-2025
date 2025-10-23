package org.example.kcdlima;

import org.springframework.boot.SpringApplication;
import org.testcontainers.Testcontainers;

public class TestServiceBApplication {

	public static void main(String[] args) {
		SpringApplication.from(KcdLima2025Application::main).with(TestcontainersConfiguration.class).run(args);
		Testcontainers.exposeHostPorts(8081);
	}

}
