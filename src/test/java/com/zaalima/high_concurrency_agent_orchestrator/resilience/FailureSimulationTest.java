package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureSimulationTest {

    private ResilienceProperties properties(long timeoutMs) {
        return new ResilienceProperties(
                50f, 6, 4, 2,
                Duration.ofMillis(100),
                50, Duration.ofMillis(100),
                1, Duration.ofMillis(10),
                Duration.ofMillis(timeoutMs));
    }

    @Test
    void slowUpstreamShouldTimeoutAndFallback() {
        ResilienceRegistry registry = new ResilienceRegistry(properties(100));
        ExternalServiceClient client = new ExternalServiceClient(registry);
        FlakyExternalService upstream = new FlakyExternalService();
        upstream.setMode(FlakyExternalService.Mode.SLOW);

        String result = client.call(
                () -> upstream.invoke(1000),
                () -> "degraded");

        assertEquals("degraded", result);
    }

    @Test
    void http500UpstreamShouldFallbackWithoutCascading() {
        ResilienceRegistry registry = new ResilienceRegistry(properties(1000));
        ExternalServiceClient client = new ExternalServiceClient(registry);
        FlakyExternalService upstream = new FlakyExternalService();
        upstream.setMode(FlakyExternalService.Mode.HTTP_500);

        AtomicInteger fallbackCount = new AtomicInteger();

        for (int i = 0; i < 6; i++) {
            client.call(
                    () -> upstream.invoke(0),
                    () -> {
                        fallbackCount.incrementAndGet();
                        return "degraded";
                    });
        }

        assertEquals(6, fallbackCount.get(),
                "every failing call must be handled by the fallback, none may leak");
        assertEquals(CircuitBreaker.State.OPEN, registry.circuitBreaker().getState());
    }

    @Test
    void disconnectAfterCircuitOpensShouldNotReachUpstream() {
        ResilienceRegistry registry = new ResilienceRegistry(properties(1000));
        ExternalServiceClient client = new ExternalServiceClient(registry);
        FlakyExternalService upstream = new FlakyExternalService();
        upstream.setMode(FlakyExternalService.Mode.DISCONNECT);

        for (int i = 0; i < 6; i++) {
            client.call(() -> upstream.invoke(0), () -> "degraded");
        }

        int callsBefore = upstream.invocations();

        String result = client.call(
                () -> upstream.invoke(0),
                () -> "short-circuited");

        assertEquals("short-circuited", result);
        assertEquals(callsBefore, upstream.invocations(),
                "open circuit must not forward to an unreachable upstream");
    }

    @Test
    void recoveryShouldCloseCircuitAfterUpstreamHeals() throws Exception {
        ResilienceRegistry registry = new ResilienceRegistry(properties(1000));
        ExternalServiceClient client = new ExternalServiceClient(registry);
        FlakyExternalService upstream = new FlakyExternalService();

        upstream.setMode(FlakyExternalService.Mode.HTTP_500);
        for (int i = 0; i < 6; i++) {
            client.call(() -> upstream.invoke(0), () -> "degraded");
        }
        assertEquals(CircuitBreaker.State.OPEN, registry.circuitBreaker().getState());

        Thread.sleep(150);

        upstream.setMode(FlakyExternalService.Mode.OK);
        AtomicInteger successes = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            String result = client.call(
                    () -> {
                        String value = upstream.invoke(0);
                        successes.incrementAndGet();
                        return value;
                    },
                    () -> "degraded");
            assertTrue(result != null);
        }

        assertEquals(CircuitBreaker.State.CLOSED, registry.circuitBreaker().getState());
        assertTrue(successes.get() > 0, "half-open probes must reach the healed upstream");
    }
}
