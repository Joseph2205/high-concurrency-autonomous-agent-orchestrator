package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.Bulkhead;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalServiceClientBulkheadTest {

    private ResilienceProperties smallBulkhead() {
        return new ResilienceProperties(
                100f, 100, 100, 3,
                Duration.ofSeconds(2),
                2, Duration.ofMillis(50),
                1, Duration.ofMillis(10),
                Duration.ofSeconds(5));
    }

    @Test
    void bulkheadShouldRejectBeyondConcurrencyLimitAndUseFallback() throws Exception {
        ResilienceRegistry registry = new ResilienceRegistry(smallBulkhead());
        ExternalServiceClient client = new ExternalServiceClient(registry);

        int callers = 20;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger fallbacks = new AtomicInteger();

        for (int i = 0; i < callers; i++) {
            executor.submit(() -> {
                String result = client.call(
                        () -> {
                            entered.countDown();
                            try {
                                release.await(2, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return "served";
                        },
                        () -> {
                            fallbacks.incrementAndGet();
                            return "rejected";
                        });
                // result intentionally ignored; we assert on counters
            });
        }

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        release.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(fallbacks.get() > 0,
                "bulkhead should reject some callers when saturated");
    }

    @Test
    void bulkheadMetricsTrackAvailableSlots() {
        ResilienceRegistry registry = new ResilienceRegistry(smallBulkhead());
        Bulkhead bulkhead = registry.bulkhead();

        assertEquals(2, bulkhead.getBulkheadConfig().getMaxConcurrentCalls());
        assertEquals(2, bulkhead.getMetrics().getAvailableConcurrentCalls());
    }

    @Test
    void fallbackShouldMapBulkheadFullToResult() {
        ResilienceRegistry registry = new ResilienceRegistry(smallBulkhead());
        ExternalServiceClient client = new ExternalServiceClient(registry);

        AtomicReference<String> seen = new AtomicReference<>();
        String result = client.call(
                () -> {
                    throw BulkheadFullException.createBulkheadFullException(
                            registry.bulkhead());
                },
                () -> "solid");

        seen.set(result);
        assertEquals("solid", seen.get());
    }
}
