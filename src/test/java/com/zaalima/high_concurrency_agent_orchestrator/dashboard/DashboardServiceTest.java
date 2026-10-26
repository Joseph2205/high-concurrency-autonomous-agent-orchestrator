package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zaalima.high_concurrency_agent_orchestrator.AbstractRedisIT;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceTest extends AbstractRedisIT {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void snapshotShouldExposeNodeAndBrokerState() {
        DashboardSnapshot snapshot = dashboardService.snapshot();

        assertNotNull(snapshot.nodeId());
        assertTrue(snapshot.timestampEpochMs() > 0);
        assertNotNull(snapshot.taskCounts());
        assertNotNull(snapshot.circuitBreakerState());
        assertTrue(snapshot.bulkheadMaxPermits() > 0);
        assertNotNull(snapshot.threads());
        assertTrue(snapshot.threads().virtualThreadExecutor(),
                "the orchestrator executor must be a virtual-thread executor");
    }
}