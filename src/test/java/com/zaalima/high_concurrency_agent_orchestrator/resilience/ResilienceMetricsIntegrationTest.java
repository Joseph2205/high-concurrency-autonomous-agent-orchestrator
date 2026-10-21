package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ResilienceMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ResilienceRegistry resilienceRegistry;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Test
    void resilienceMetersShouldBeRegistered() {
        assertTrue(
                meterRegistry.getMeters().stream().anyMatch(
                        meter -> meter.getId().getName()
                                .startsWith("resilience4j.circuitbreaker")),
                "circuit breaker meters must be bound to Micrometer");
    }

    @Test
    void circuitBreakerMetersShouldExposeStateAfterFailure() {
        assertThrows(RuntimeException.class, () -> externalServiceClient.call(() -> {
            throw new IllegalStateException("boom");
        }, null));

        double failed = meterRegistry.get("resilience4j.circuitbreaker.calls")
                .tag("name", ResilienceRegistry.EXTERNAL_CALL)
                .tag("kind", "failed")
                .timer()
                .count();

        assertTrue(failed >= 1, "failed-call timer count must increment");
    }
}
