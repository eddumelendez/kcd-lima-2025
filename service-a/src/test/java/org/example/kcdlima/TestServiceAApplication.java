package org.example.kcdlima;

import org.springframework.boot.SpringApplication;

public class TestServiceAApplication {

	public static void main(String[] args) {
		SpringApplication.from(KcdLima2025Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
