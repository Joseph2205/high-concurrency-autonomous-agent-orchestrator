package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import com.zaalima.high_concurrency_agent_orchestrator.cluster.NodeInfo;
import com.zaalima.high_concurrency_agent_orchestrator.cluster.RedisNodeRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis"
)
public class ClusterNodeProvider {

    private final RedisNodeRegistry nodeRegistry;
    private final long staleAfterMs;

    public ClusterNodeProvider(
            RedisNodeRegistry nodeRegistry,
            @Value("${orchestrator.registry.stale-after-seconds:15}")
            long staleAfterSeconds
    ) {
        this.nodeRegistry = nodeRegistry;
        this.staleAfterMs = staleAfterSeconds * 1000L;
    }

    /**
     * Retrieves all nodes and determines their health status.
     */
    public List<DashboardSnapshot.NodeView> clusterNodes() {

        List<NodeInfo> registeredNodes = nodeRegistry.findAll();

        if (registeredNodes.isEmpty()) {
            return List.of();
        }

        long now = System.currentTimeMillis();

        List<DashboardSnapshot.NodeView> nodes =
                new ArrayList<>(registeredNodes.size());

        for (NodeInfo node : registeredNodes) {

            long heartbeatAge = now - node.lastHeartbeatEpochMs();

            boolean alive = heartbeatAge >= 0
                    && heartbeatAge <= staleAfterMs;

            nodes.add(
                    new DashboardSnapshot.NodeView(
                            node.nodeId(),
                            alive,
                            node.activeTasks()
                    )
            );
        }

        return List.copyOf(nodes);
    }
}