package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalServiceClientCircuitBreakerTest {

    private ResilienceProperties fastProperties() {
        return new ResilienceProperties(
                50f, 4, 4, 2,
                java.time.Duration.ofSeconds(2),
                50, java.time.Duration.ofMillis(100),
                1, java.time.Duration.ofMillis(10),
                java.time.Duration.ofSeconds(1));
    }

    @Test
    void circuitShouldOpenAfterConsecutiveFailures() {
        ResilienceRegistry registry = new ResilienceRegistry(fastProperties());
        ExternalServiceClient client = new ExternalServiceClient(registry);

        AtomicInteger attempts = new AtomicInteger();

        for (int i = 0; i < 4; i++) {
            assertThrows(RuntimeException.class,
                    () -> client.call(() -> {
                        attempts.incrementAndGet();
                        throw new IllegalStateException("upstream down");
                    }, null));
        }

        assertEquals(CircuitBreaker.State.OPEN, registry.circuitBreaker().getState());

        assertThrows(RuntimeException.class,
                () -> client.call(() -> {
                    attempts.incrementAndGet();
                    return "should not run";
                }, null));

        assertTrue(attempts.get() <= 4,
                "open circuit must stop forwarding calls upstream");
    }

    @Test
    void fallbackShouldBeReturnedWhenCallFails() {
        ResilienceRegistry registry = new ResilienceRegistry(fastProperties());
        ExternalServiceClient client = new ExternalServiceClient(registry);

        String result = client.call(
                () -> {
                    throw new IllegalStateException("boom");
                },
                () -> "fallback-value");

        assertEquals("fallback-value", result);
    }

    @Test
    void successfulCallShouldReturnValue() {
        ResilienceRegistry registry = new ResilienceRegistry(fastProperties());
        ExternalServiceClient client = new ExternalServiceClient(registry);

        String result = client.call(() -> "ok", () -> "fallback");

        assertEquals("ok", result);
    }
}
