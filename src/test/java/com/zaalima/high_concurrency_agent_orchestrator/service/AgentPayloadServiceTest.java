package com.zaalima.high_concurrency_agent_orchestrator.service;

import com.zaalima.high_concurrency_agent_orchestrator.AgentTaskService;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentInfo;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.agent.InMemoryAgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.controller.AgentRegistrationController.AgentNotRegisteredException;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPayloadServiceTest {

    private AgentPayloadService newService(AgentRegistry registry, ExecutorService executor) {
        return new AgentPayloadService(registry, new AgentTaskService(executor));
    }

    @Test
    void registeredAgentPayloadShouldCreateTask() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AgentRegistry registry = new InMemoryAgentRegistry("test-node");
            registry.register("agent-1", "test", null, Map.of());
            AgentPayloadService service = newService(registry, executor);

            AgentPayloadResponse response = service.submit(
                    "agent-1",
                    "fetch-analytics",
                    Map.of("userId", "42"));

            assertTrue(response.accepted());
            assertNotNull(response.taskId());
        }
    }

    @Test
    void unknownAgentPayloadShouldBeRejected() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AgentRegistry registry = new InMemoryAgentRegistry("test-node");
            AgentPayloadService service = newService(registry, executor);

            assertThrows(
                    AgentNotRegisteredException.class,
                    () -> service.submit("ghost", "task", Map.of()));
        }
    }

    @Test
    void payloadShouldBeMergedIntoTaskName() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AgentRegistry registry = new InMemoryAgentRegistry("test-node");
            registry.register("agent-2", "test", null, Map.of());
            AgentTaskService taskService = new AgentTaskService(executor);
            AgentPayloadService service = new AgentPayloadService(registry, taskService);

            AgentPayloadResponse response = service.submit(
                    "agent-2",
                    "job",
                    Map.of("x", "1"));

            assertEquals("job {x=1}", taskService.getTask(response.taskId()).getTaskName());
        }
    }
}
