package com.zaalima.high_concurrency_agent_orchestrator.cluster;

/**
 * Represents the current state and resource usage of an orchestrator node.
 * Each running orchestrator instance publishes its NodeInfo to Redis.
 *
 * @param nodeId unique identifier of this node
 * @param lastHeartbeatEpochMs timestamp of the latest heartbeat
 * @param activeTasks number of tasks currently executing on this node
 * @param maxConcurrentTasks maximum tasks this node can execute concurrently
 * @param cpuLoad CPU utilization percentage (0-100)
 * @param memoryUsagePercent memory utilization percentage (0-100)
 */
public record NodeInfo(
        String nodeId,
        long lastHeartbeatEpochMs,
        long activeTasks,
        long maxConcurrentTasks,
        double cpuLoad,
        double memoryUsagePercent
) {

    /**
     * Calculates the current node load percentage.
     *
     * @return load percentage between 0 and 100
     */
    public double loadPercentage() {

        if (maxConcurrentTasks <= 0) {
            return 0.0;
        }

        return Math.min(
                100.0,
                (activeTasks * 100.0) / maxConcurrentTasks
        );
    }

    /**
     * Checks whether the node has available task capacity.
     *
     * @return true if the node can accept another task
     */
    public boolean hasAvailableCapacity() {
        return activeTasks < maxConcurrentTasks;
    }
}