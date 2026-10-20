package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HighConcurrencyAgentOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(HighConcurrencyAgentOrchestratorApplication.class, args);
	}

}
