package com.zaalima.high_concurrency_agent_orchestrator.agent;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentSnapshotTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void snapshotShouldRoundTripThroughJson() {
        AgentInfo original = new AgentInfo(
                "agent-1",
                "name",
                "http://localhost:9000",
                Map.of("region", "test"));
        original.recordHeartbeat(7);

        String json = objectMapper.writeValueAsString(AgentSnapshot.from(original));
        AgentSnapshot snapshot = objectMapper.readValue(json, AgentSnapshot.class);
        AgentInfo restored = snapshot.toAgentInfo();

        assertEquals("agent-1", restored.getAgentId());
        assertEquals("name", restored.getAgentName());
        assertEquals("http://localhost:9000", restored.getEndpoint());
        assertEquals(Map.of("region", "test"), restored.getMetadata());
        assertEquals(7, restored.getActiveTasks());
        assertEquals(original.getRegisteredAtEpochMs(), restored.getRegisteredAtEpochMs());
        assertEquals(original.getLastHeartbeatEpochMs(), restored.getLastHeartbeatEpochMs());
    }
}
