package com.zaalima.high_concurrency_agent_orchestrator.simulator;

import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentHeartbeatRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(
        name = "orchestrator.simulator.enabled",
        havingValue = "true")
public class FakeAgentSimulator implements ApplicationRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(FakeAgentSimulator.class);

    private final RestClient restClient;
    private final SimulatorProperties properties;

    public FakeAgentSimulator(RestClient.Builder builder,
                              SimulatorProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        int agentCount = properties.agentCount();
        logger.info("Fake-agent simulator starting {} agents against {}",
                agentCount, properties.baseUrl());

        AtomicInteger registered = new AtomicInteger();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < agentCount; i++) {
                int index = i;
                executor.submit(() -> simulateAgent(index, registered));
            }
        }

        logger.info("Fake-agent simulator finished: {} agents completed", registered.get());
    }

    private void simulateAgent(int index, AtomicInteger registered) {
        String agentId = "sim-agent-" + index;

        try {
            restClient.post()
                    .uri("/api/agents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AgentRegistrationRequest(
                            agentId,
                            "simulated-agent",
                            "http://localhost:0/sim",
                            Map.of("sim", "true")))
                    .retrieve()
                    .toBodilessEntity();

            int submissions = ThreadLocalRandom.current()
                    .nextInt(1, properties.maxSubmissionsPerAgent() + 1);

            for (int s = 0; s < submissions; s++) {
                restClient.post()
                        .uri("/api/agents/payload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AgentPayloadRequest(
                                agentId,
                                "sim-task-" + index + "-" + s,
                                Map.of("weight", String.valueOf(s))))
                        .retrieve()
                        .toBodilessEntity();
            }

            restClient.post()
                    .uri("/api/agents/heartbeat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AgentHeartbeatRequest(agentId, (long) submissions))
                    .retrieve()
                    .toBodilessEntity();

            registered.incrementAndGet();

        } catch (RuntimeException e) {
            logger.warn("Simulated agent {} failed: {}", agentId, e.getMessage());
        }
    }
}
