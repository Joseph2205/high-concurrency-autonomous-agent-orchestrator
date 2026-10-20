package com.zaalima.high_concurrency_agent_orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentHeartbeatRequest(
        @NotBlank String agentId,
        long activeTasks) {
}
