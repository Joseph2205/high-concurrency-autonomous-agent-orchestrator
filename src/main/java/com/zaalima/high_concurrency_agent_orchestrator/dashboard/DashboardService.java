package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentInfo;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.resilience.ResilienceRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
public class DashboardService {

    private final AgentRegistry registry;
    private final ResilienceRegistry resilienceRegistry;
    private final DashboardTaskProvider taskProvider;
    private final VirtualThreadMonitor threadMonitor;
    private final ExecutorService virtualThreadExecutor;
    private final ObjectProvider<ClusterNodeProvider> clusterNodeProvider;

    public DashboardService(
            AgentRegistry registry,
            ResilienceRegistry resilienceRegistry,
            DashboardTaskProvider taskProvider,
            VirtualThreadMonitor threadMonitor,
            ExecutorService virtualThreadExecutor,
            ObjectProvider<ClusterNodeProvider> clusterNodeProvider) {
        this.registry = registry;
        this.resilienceRegistry = resilienceRegistry;
        this.taskProvider = taskProvider;
        this.threadMonitor = threadMonitor;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.clusterNodeProvider = clusterNodeProvider;
    }

    public DashboardSnapshot snapshot() {
        CircuitBreaker breaker = resilienceRegistry.circuitBreaker();
        Bulkhead bulkhead = resilienceRegistry.bulkhead();
        List<AgentInfo> agents = registry.findAll();
        ClusterNodeProvider nodeProvider = clusterNodeProvider.getIfAvailable();

        return new DashboardSnapshot(
                registry.getNodeId(),
                System.currentTimeMillis(),
                agents.size(),
                agents.stream().mapToLong(AgentInfo::getActiveTasks).sum(),
                taskProvider.taskCounts(),
                taskProvider.recentTasks(25),
                nodeProvider == null ? List.of() : nodeProvider.clusterNodes(),
                breaker.getState().name(),
                breaker.getMetrics().getFailureRate(),
                bulkhead.getMetrics().getAvailableConcurrentCalls(),
                bulkhead.getMetrics().getMaxAllowedConcurrentCalls(),
                threadMonitor.sample(virtualThreadExecutor));
    }
}
