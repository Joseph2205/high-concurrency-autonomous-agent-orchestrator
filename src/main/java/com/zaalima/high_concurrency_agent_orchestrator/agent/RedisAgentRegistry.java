package com.zaalima.high_concurrency_agent_orchestrator.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis")
public class RedisAgentRegistry implements AgentRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisAgentRegistry.class);

    static final String AGENTS_KEY = "orchestrator:agents";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String nodeId;

    public RedisAgentRegistry(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${orchestrator.node-id:node-local}") String nodeId) {

        this.redis = redis;
        this.objectMapper = objectMapper;
        this.nodeId = nodeId;
    }

    @Override
    public AgentInfo register(
            String agentId,
            String agentName,
            String endpoint,
            Map<String, String> metadata) {

        AgentInfo info = new AgentInfo(agentId, agentName, endpoint, metadata);
        redis.opsForHash().put(AGENTS_KEY, agentId, toJson(info));
        logger.debug("Registry: wrote agent {} to Redis hash {}", agentId, AGENTS_KEY);
        return info;
    }

    @Override
    public boolean heartbeat(String agentId, long activeTasks) {
        Object raw = redis.opsForHash().get(AGENTS_KEY, agentId);
        if (raw == null) {
            return false;
        }

        AgentInfo info = fromJson(raw.toString());
        if (info == null) {
            return false;
        }

        info.recordHeartbeat(activeTasks);
        redis.opsForHash().put(AGENTS_KEY, agentId, toJson(info));
        return true;
    }

    @Override
    public boolean deregister(String agentId) {
        Long removed = redis.opsForHash().delete(AGENTS_KEY, agentId);
        return removed != null && removed > 0;
    }

    @Override
    public Optional<AgentInfo> find(String agentId) {
        Object raw = redis.opsForHash().get(AGENTS_KEY, agentId);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fromJson(raw.toString()));
    }

    @Override
    public List<AgentInfo> findAll() {
        List<Object> values = redis.opsForHash().values(AGENTS_KEY);
        List<AgentInfo> result = new ArrayList<>(values.size());
        for (Object raw : values) {
            AgentInfo info = fromJson(raw.toString());
            if (info != null) {
                result.add(info);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    private String toJson(AgentInfo info) {
        return objectMapper.writeValueAsString(AgentSnapshot.from(info));
    }

    private AgentInfo fromJson(String json) {
        try {
            AgentSnapshot snapshot = objectMapper.readValue(json, AgentSnapshot.class);
            return snapshot.toAgentInfo();
        } catch (RuntimeException e) {
            logger.warn("Registry: skipping unreadable agent record: {}", e.getMessage());
            return null;
        }
    }
}
