package com.zaalima.high_concurrency_agent_orchestrator.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AgentRegistryService {

    private final ConcurrentMap<String, AgentInfo> agents = new ConcurrentHashMap<>();
    private final String nodeId;

    public AgentRegistryService(
            @Value("${orchestrator.node-id:node-local}") String nodeId) {
        this.nodeId = nodeId;
    }

    public AgentInfo register(
            String agentId,
            String agentName,
            String endpoint,
            Map<String, String> metadata) {

        AgentInfo info = new AgentInfo(agentId, agentName, endpoint, metadata);
        agents.put(agentId, info);
        return info;
    }

    public boolean heartbeat(String agentId, long activeTasks) {
        AgentInfo info = agents.get(agentId);
        if (info == null) {
            return false;
        }
        info.recordHeartbeat(activeTasks);
        return true;
    }

    public boolean deregister(String agentId) {
        return agents.remove(agentId) != null;
    }

    public Optional<AgentInfo> find(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    public List<AgentInfo> findAll() {
        return List.copyOf(agents.values());
    }

    public String getNodeId() {
        return nodeId;
    }
}
