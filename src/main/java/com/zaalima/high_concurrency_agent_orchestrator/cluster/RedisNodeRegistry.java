package com.zaalima.high_concurrency_agent_orchestrator.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis"
)
public class RedisNodeRegistry {

    private static final Logger logger =
            LoggerFactory.getLogger(RedisNodeRegistry.class);

    private static final String NODES_KEY = "orchestrator:nodes";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisNodeRegistry(
            StringRedisTemplate redis,
            ObjectMapper objectMapper
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves or updates a node in Redis.
     */
    public void save(NodeInfo nodeInfo) {

        String json = objectMapper.writeValueAsString(nodeInfo);

        redis.opsForHash().put(
                NODES_KEY,
                nodeInfo.nodeId(),
                json
        );

        logger.debug(
                "Node registry: saved node {} to Redis",
                nodeInfo.nodeId()
        );
    }

    /**
     * Finds a node using its unique node ID.
     */
    public Optional<NodeInfo> find(String nodeId) {

        Object raw = redis.opsForHash().get(NODES_KEY, nodeId);

        if (raw == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(fromJson(raw.toString()));
    }

    /**
     * Retrieves all registered nodes.
     */
    public List<NodeInfo> findAll() {

        List<Object> values = redis.opsForHash().values(NODES_KEY);

        List<NodeInfo> nodes = new ArrayList<>(values.size());

        for (Object raw : values) {

            NodeInfo nodeInfo = fromJson(raw.toString());

            if (nodeInfo != null) {
                nodes.add(nodeInfo);
            }
        }

        return List.copyOf(nodes);
    }

    /**
     * Removes a node from Redis.
     */
    public boolean remove(String nodeId) {

        Long removed = redis.opsForHash().delete(NODES_KEY, nodeId);

        return removed != null && removed > 0;
    }

    /**
     * Converts NodeInfo into JSON.
     */
    private NodeInfo fromJson(String json) {

        try {

            return objectMapper.readValue(json, NodeInfo.class);

        } catch (RuntimeException exception) {

            logger.warn(
                    "Node registry: unable to read node record: {}",
                    exception.getMessage()
            );

            return null;
        }
    }
}