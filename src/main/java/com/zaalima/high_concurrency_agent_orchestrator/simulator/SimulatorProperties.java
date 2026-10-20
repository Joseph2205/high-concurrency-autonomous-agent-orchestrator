package com.zaalima.high_concurrency_agent_orchestrator.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator.simulator")
public record SimulatorProperties(
        boolean enabled,
        int agentCount,
        int maxSubmissionsPerAgent,
        String baseUrl) {

    public SimulatorProperties {
        if (agentCount <= 0) {
            agentCount = 100;
        }
        if (maxSubmissionsPerAgent <= 0) {
            maxSubmissionsPerAgent = 3;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080";
        }
    }
}
