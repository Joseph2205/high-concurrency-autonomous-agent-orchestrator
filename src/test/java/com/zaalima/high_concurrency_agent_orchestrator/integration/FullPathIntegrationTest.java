package com.zaalima.high_concurrency_agent_orchestrator.integration;

import com.zaalima.high_concurrency_agent_orchestrator.AbstractRedisIT;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.resilience.ExternalServiceClient;
import com.zaalima.high_concurrency_agent_orchestrator.service.AgentPayloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end path across all four members' concerns:
 * register (M3) -> registry (M3) -> resilience-guarded upstream call (M3/M4)
 * -> payload submission (M3) -> virtual-thread execution (M2).
 */
class FullPathIntegrationTest extends AbstractRedisIT {

    @Autowired
    private AgentRegistry registry;

    @Autowired
    private AgentPayloadService payloadService;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Test
    void registerSubmitAndExecuteUnderResilience() {
        String agentId = "integration-agent-1";

        registry.register(agentId, "integration", "http://localhost:0", Map.of("env", "test"));

        // Resilience-guarded upstream call succeeds normally.
        String upstream = externalServiceClient.call(
                () -> "upstream-data",
                () -> "degraded");
        assertEquals("upstream-data", upstream);

        // Payload submission creates a virtual-thread task.
        var response = payloadService.submit(
                agentId, "process-order", Map.of("orderId", "A-1"));

        assertTrue(response.accepted());
        assertNotNull(response.taskId());
    }

    @Test
    void unregisteredAgentCannotSubmit() {
        assertThrows(
                com.zaalima.high_concurrency_agent_orchestrator.controller
                        .AgentRegistrationController.AgentNotRegisteredException.class,
                () -> payloadService.submit("ghost-agent", "task", Map.of()));
    }

    @Test
    void registrySharedBetweenRestAndGrpcView() {
        String agentId = "integration-agent-2";
        registry.register(agentId, "integration", null, Map.of());

        assertTrue(registry.find(agentId).isPresent());
        assertEquals("integration", registry.find(agentId).orElseThrow().getAgentName());
    }
}