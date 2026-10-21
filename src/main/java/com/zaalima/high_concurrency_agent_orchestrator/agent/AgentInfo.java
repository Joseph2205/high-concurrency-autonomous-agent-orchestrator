package com.zaalima.high_concurrency_agent_orchestrator.agent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class AgentInfo {

    private final String agentId;
    private final String agentName;
    private final String endpoint;
    private final Map<String, String> metadata;
    private volatile long registeredAtEpochMs;

    private final AtomicLong activeTasks = new AtomicLong();
    private volatile long lastHeartbeatEpochMs;

    public AgentInfo(
            String agentId,
            String agentName,
            String endpoint,
            Map<String, String> metadata) {

        this.agentId = agentId;
        this.agentName = agentName;
        this.endpoint = endpoint;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);

        long now = System.currentTimeMillis();
        this.registeredAtEpochMs = now;
        this.lastHeartbeatEpochMs = now;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public long getRegisteredAtEpochMs() {
        return registeredAtEpochMs;
    }

    public long getLastHeartbeatEpochMs() {
        return lastHeartbeatEpochMs;
    }

    public long getActiveTasks() {
        return activeTasks.get();
    }

    public void recordHeartbeat(long currentActiveTasks) {
        this.activeTasks.set(currentActiveTasks);
        this.lastHeartbeatEpochMs = System.currentTimeMillis();
    }

    void restoreTimestamps(long registeredAt, long lastHeartbeat, long currentActiveTasks) {
        this.registeredAtEpochMs = registeredAt;
        this.lastHeartbeatEpochMs = lastHeartbeat;
        this.activeTasks.set(currentActiveTasks);
    }
}
