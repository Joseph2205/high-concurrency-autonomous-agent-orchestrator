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

    // Logger is used to record application events.
    // It helps developers monitor payload submissions
    // and troubleshoot problems.
    private static final Logger logger =
            LoggerFactory.getLogger(AgentPayloadService.class);

    // AgentRegistry checks whether an agent is registered
    // in the orchestrator.
    private final AgentRegistry registry;

    // AgentTaskService handles task creation and submission.
    private final AgentTaskService taskService;

    // Constructor injection.
    // Spring automatically injects the required dependencies.
    public AgentPayloadService(
            AgentRegistry registry,
            AgentTaskService taskService) {

        this.registry = registry;
        this.taskService = taskService;
    }

    /**
     * Receives a payload from an agent and converts it
     * into a task that can be processed by the orchestrator.
     *
     * @param agentId  Unique ID of the submitting agent.
     * @param taskName Name of the requested task.
     * @param payload  Additional data sent by the agent.
     *
     * @return AgentPayloadResponse containing submission details.
     */
    public AgentPayloadResponse submit(
            String agentId,
            String taskName,
            Map<String, String> payload) {

        // STEP 1: Verify that the agent is registered.
        //
        // registry.find(agentId) returns an Optional.
        // isEmpty() checks whether the agent does not exist.
        //
        // If the agent is not registered, reject the request.
        if (registry.find(agentId).isEmpty()) {

            throw new AgentNotRegisteredException(agentId);
        }

        // STEP 2: Enrich the task name with additional payload data.
        //
        // Example:
        // taskName = "ProcessPayment"
        // payload  = {amount=500, currency=INR}
        //
        // enrichedTaskName becomes:
        // ProcessPayment {amount=500, currency=INR}
        String enrichedTaskName = enrich(taskName, payload);

        // STEP 3: Submit the enriched task to AgentTaskService.
        //
        // submitTask() creates/submits the task and returns
        // an AgentTask object containing task information.
        AgentTask task = taskService.submitTask(enrichedTaskName);

        // STEP 4: Log successful payload submission.
        //
        // {} placeholders are populated by SLF4J.
        // This avoids unnecessary string concatenation.
        logger.info(
                "Payload from agent {} accepted as task {} (status={})",
                agentId,
                task.getTaskId(),
                task.getStatus());

        // STEP 5: Build the response.
        //
        // true             -> Submission accepted.
        // task.getTaskId() -> Generated task identifier.
        // task.getStatus() -> Current task status.
        // Message          -> Human-readable confirmation.
        return new AgentPayloadResponse(
                true,
                task.getTaskId(),
                task.getStatus(),
                "Payload accepted and queued");
    }

    /**
     * Adds payload information to the task name.
     */
    private String enrich(
            String taskName,
            Map<String, String> payload) {

        // If no additional payload is provided,
        // return the original task name.
        if (payload == null || payload.isEmpty()) {
            return taskName;
        }

        // Append payload information to the task name.
        return taskName + " " + payload;
    }
}