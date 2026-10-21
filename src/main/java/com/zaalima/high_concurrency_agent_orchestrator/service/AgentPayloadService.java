package com.zaalima.high_concurrency_agent_orchestrator.service;

import com.zaalima.high_concurrency_agent_orchestrator.AgentTask;
import com.zaalima.high_concurrency_agent_orchestrator.AgentTaskService;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.controller.AgentRegistrationController.AgentNotRegisteredException;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AgentPayloadService {

    private static final Logger logger =
            LoggerFactory.getLogger(AgentPayloadService.class);

    private final AgentRegistry registry;
    private final AgentTaskService taskService;

    public AgentPayloadService(AgentRegistry registry, AgentTaskService taskService) {
        this.registry = registry;
        this.taskService = taskService;
    }

    public AgentPayloadResponse submit(
            String agentId,
            String taskName,
            Map<String, String> payload) {

        if (registry.find(agentId).isEmpty()) {
            throw new AgentNotRegisteredException(agentId);
        }

        String enrichedTaskName = enrich(taskName, payload);
        AgentTask task = taskService.submitTask(enrichedTaskName);

        logger.info(
                "Payload from agent {} accepted as task {} (status={})",
                agentId,
                task.getTaskId(),
                task.getStatus());

        return new AgentPayloadResponse(
                true,
                task.getTaskId(),
                task.getStatus(),
                "Payload accepted and queued");
    }

    private String enrich(String taskName, Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return taskName;
        }
        return taskName + " " + payload;
    }
}
