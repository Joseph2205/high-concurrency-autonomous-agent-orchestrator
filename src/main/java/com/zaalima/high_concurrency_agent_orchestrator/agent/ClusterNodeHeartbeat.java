package com.zaalima.high_concurrency_agent_orchestrator.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes this node's liveness into the shared {@code orchestrator:nodes}
 * hash so any dashboard in the cluster can render all nodes.
 */
@Component
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis")
public class ClusterNodeHeartbeat {

    private static final Logger logger =
            LoggerFactory.getLogger(ClusterNodeHeartbeat.class);

    static final String NODES_KEY = "orchestrator:nodes";

    private final StringRedisTemplate redis;
    private final AgentRegistry registry;

    public ClusterNodeHeartbeat(StringRedisTemplate redis, AgentRegistry registry) {
        this.redis = redis;
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${orchestrator.registry.node-heartbeat-ms:5000}")
    public void publish() {
        long activeTasks = registry.findAll().stream()
                .mapToLong(info -> info.getActiveTasks())
                .sum();

        String value = System.currentTimeMillis() + "|" + activeTasks;
        redis.opsForHash().put(NODES_KEY, registry.getNodeId(), value);
        logger.debug("Published node heartbeat {} -> {}", registry.getNodeId(), value);
    }
}
