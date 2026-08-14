package com.tieto.poc.ai_servicenow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiServicenowApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiServicenowApplication.class, args);
	}

}
