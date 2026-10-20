package com.zaalima.high_concurrency_agent_orchestrator.dto;

public record AgentRegistrationResponse(
        boolean accepted,
        String message,
        String nodeId) {
}
