package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentInfo;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.resilience.ResilienceRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardService {

    private final AgentRegistry registry;
    private final ResilienceRegistry resilienceRegistry;
    private final DashboardTaskProvider taskProvider;

    public DashboardService(
            AgentRegistry registry,
            ResilienceRegistry resilienceRegistry,
            DashboardTaskProvider taskProvider) {
        this.registry = registry;
        this.resilienceRegistry = resilienceRegistry;
        this.taskProvider = taskProvider;
    }

    public DashboardSnapshot snapshot() {
        CircuitBreaker breaker = resilienceRegistry.circuitBreaker();
        Bulkhead bulkhead = resilienceRegistry.bulkhead();

        return new DashboardSnapshot(
                registry.getNodeId(),
                System.currentTimeMillis(),
                registry.findAll().size(),
                registry.findAll().stream().mapToLong(AgentInfo::getActiveTasks).sum(),
                taskProvider.taskCounts(),
                taskProvider.recentTasks(25),
                List.of(),
                breaker.getState().name(),
                breaker.getMetrics().getFailureRate(),
                bulkhead.getMetrics().getAvailableConcurrentCalls(),
                bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
    }
}
