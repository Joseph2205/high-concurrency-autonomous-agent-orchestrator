package com.zaalima.high_concurrency_agent_orchestrator.dto;

public record AgentPayloadResponse(
        boolean accepted,
        String taskId,
        String status,
        String message) {
}
