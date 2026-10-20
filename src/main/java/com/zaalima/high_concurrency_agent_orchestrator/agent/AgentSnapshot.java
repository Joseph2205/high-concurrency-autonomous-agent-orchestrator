package com.zaalima.high_concurrency_agent_orchestrator.agent;

import java.util.Map;

public record AgentSnapshot(
        String agentId,
        String agentName,
        String endpoint,
        Map<String, String> metadata,
        long registeredAtEpochMs,
        long lastHeartbeatEpochMs,
        long activeTasks) {

    static AgentSnapshot from(AgentInfo info) {
        return new AgentSnapshot(
                info.getAgentId(),
                info.getAgentName(),
                info.getEndpoint(),
                info.getMetadata(),
                info.getRegisteredAtEpochMs(),
                info.getLastHeartbeatEpochMs(),
                info.getActiveTasks());
    }

    AgentInfo toAgentInfo() {
        AgentInfo info = new AgentInfo(agentId, agentName, endpoint, metadata);
        info.restoreTimestamps(registeredAtEpochMs, lastHeartbeatEpochMs, activeTasks);
        return info;
    }
}
