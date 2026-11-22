package com.zaalima.high_concurrency_agent_orchestrator.agent;

import com.zaalima.high_concurrency_agent_orchestrator.cluster.NodeInfo;
import com.zaalima.high_concurrency_agent_orchestrator.cluster.NodeTaskTracker;
import com.zaalima.high_concurrency_agent_orchestrator.cluster.RedisNodeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "orchestrator.registry.mode",
        havingValue = "redis"
)
public class ClusterNodeHeartbeat {

    private static final Logger logger =
            LoggerFactory.getLogger(ClusterNodeHeartbeat.class);

    private final AgentRegistry agentRegistry;
    private final RedisNodeRegistry nodeRegistry;
    private final NodeTaskTracker nodeTaskTracker;
    private final long maxConcurrentTasks;

    public ClusterNodeHeartbeat(
            AgentRegistry agentRegistry,
            RedisNodeRegistry nodeRegistry,
            NodeTaskTracker nodeTaskTracker,
            @Value("${orchestrator.node.max-concurrent-tasks:100}")
            long maxConcurrentTasks
    ) {
        this.agentRegistry = agentRegistry;
        this.nodeRegistry = nodeRegistry;
        this.nodeTaskTracker = nodeTaskTracker;
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    /**
     * Publishes this node's heartbeat and load information to Redis.
     */
    @Scheduled(
            fixedDelayString = "${orchestrator.registry.node-heartbeat-ms:5000}"
    )
    public void publish() {

        long activeTasks = nodeTaskTracker.getActiveTasks();

        NodeInfo nodeInfo = new NodeInfo(
                agentRegistry.getNodeId(),
                System.currentTimeMillis(),
                activeTasks,
                maxConcurrentTasks,
                0.0,
                0.0
        );

        nodeRegistry.save(nodeInfo);

        logger.debug(
                "Published node heartbeat: nodeId={}, activeTasks={}, load={}%",
                nodeInfo.nodeId(),
                nodeInfo.activeTasks(),
                nodeInfo.loadPercentage()
        );
    }
}