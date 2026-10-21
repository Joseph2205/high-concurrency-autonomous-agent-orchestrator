package com.zaalima.high_concurrency_agent_orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record AgentPayloadRequest(
        @NotBlank String agentId,
        @NotBlank String taskName,
        Map<String, String> payload) {
}
