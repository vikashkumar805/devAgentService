package com.hackathon.aicodefixer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class DevAgentServiceApplication {

	public static void main(String[] args) {

		log.info("Starting Application DevAgent Service");
		SpringApplication.run(DevAgentServiceApplication.class, args);
	}

}
