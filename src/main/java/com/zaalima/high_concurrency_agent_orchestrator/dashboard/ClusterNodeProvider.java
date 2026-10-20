package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the cluster node view published by Member 1's node registry.
 * Nodes write {@code orchestrator:nodes} hash entries as
 * {@code nodeId -> "lastHeartbeatEpochMs|activeTasks"}.
 */
@Component
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis")
public class ClusterNodeProvider {

    static final String NODES_KEY = "orchestrator:nodes";

    private final StringRedisTemplate redis;
    private final long staleAfterMs;

    public ClusterNodeProvider(
            StringRedisTemplate redis,
            @Value("${orchestrator.registry.stale-after-seconds:15}")
            long staleAfterSeconds) {
        this.redis = redis;
        this.staleAfterMs = staleAfterSeconds * 1000L;
    }

    public List<DashboardSnapshot.NodeView> clusterNodes() {
        Map<Object, Object> entries = redis.opsForHash().entries(NODES_KEY);
        if (entries.isEmpty()) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        List<DashboardSnapshot.NodeView> nodes = new ArrayList<>(entries.size());

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String nodeId = String.valueOf(entry.getKey());
            long lastHeartbeat = parseHeartbeat(String.valueOf(entry.getValue()));
            boolean alive = (now - lastHeartbeat) <= staleAfterMs;
            nodes.add(new DashboardSnapshot.NodeView(
                    nodeId, alive, parseActiveTasks(String.valueOf(entry.getValue()))));
        }

        return List.copyOf(nodes);
    }

    private long parseHeartbeat(String value) {
        String[] parts = value.split("\\|");
        try {
            return Long.parseLong(parts[0]);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private long parseActiveTasks(String value) {
        String[] parts = value.split("\\|");
        if (parts.length < 2) {
            return 0L;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (RuntimeException e) {
            return 0L;
        }
    }
}
