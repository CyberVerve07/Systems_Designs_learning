package com.systemdesign.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Day1ClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(Day1ClientApplication.class, args);
	}

	// RestTemplate bean to make HTTP calls to our "Load Balancer" or "Server"
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
