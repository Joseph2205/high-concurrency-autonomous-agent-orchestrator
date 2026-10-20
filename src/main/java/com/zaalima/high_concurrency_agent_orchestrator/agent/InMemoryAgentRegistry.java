package com.zaalima.high_concurrency_agent_orchestrator.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "memory",
        matchIfMissing = true)
public class InMemoryAgentRegistry implements AgentRegistry {

    private final ConcurrentMap<String, AgentInfo> agents = new ConcurrentHashMap<>();
    private final String nodeId;

    public InMemoryAgentRegistry(
            @Value("${orchestrator.node-id:node-local}") String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public AgentInfo register(
            String agentId,
            String agentName,
            String endpoint,
            Map<String, String> metadata) {

        AgentInfo info = new AgentInfo(agentId, agentName, endpoint, metadata);
        agents.put(agentId, info);
        return info;
    }

    @Override
    public boolean heartbeat(String agentId, long activeTasks) {
        AgentInfo info = agents.get(agentId);
        if (info == null) {
            return false;
        }
        info.recordHeartbeat(activeTasks);
        return true;
    }

    @Override
    public boolean deregister(String agentId) {
        return agents.remove(agentId) != null;
    }

    @Override
    public Optional<AgentInfo> find(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    @Override
    public List<AgentInfo> findAll() {
        return List.copyOf(agents.values());
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }
}
