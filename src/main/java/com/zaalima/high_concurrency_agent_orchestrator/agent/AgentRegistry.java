package com.zaalima.high_concurrency_agent_orchestrator.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AgentRegistry {

    AgentInfo register(
            String agentId,
            String agentName,
            String endpoint,
            Map<String, String> metadata);

    boolean heartbeat(String agentId, long activeTasks);

    boolean deregister(String agentId);

    Optional<AgentInfo> find(String agentId);

    List<AgentInfo> findAll();

    String getNodeId();
}
