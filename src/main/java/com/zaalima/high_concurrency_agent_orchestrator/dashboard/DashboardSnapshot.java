package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import java.util.List;

public record DashboardSnapshot(
        String nodeId,
        long timestampEpochMs,
        int registeredAgents,
        long activeTasksReportedByAgents,
        TaskCounts taskCounts,
        List<TaskView> recentTasks,
        List<NodeView> clusterNodes,
        String circuitBreakerState,
        float circuitBreakerFailureRate,
        int bulkheadAvailablePermits,
        int bulkheadMaxPermits,
        VirtualThreadMonitor.VirtualThreadStats threads) {

    public record TaskCounts(long running, long completed, long failed) {
    }

    public record TaskView(
            String taskId,
            String taskName,
            String status,
            boolean virtualThread) {
    }

    public record NodeView(
            String nodeId,
            boolean alive,
            long activeTasks) {
    }
}
