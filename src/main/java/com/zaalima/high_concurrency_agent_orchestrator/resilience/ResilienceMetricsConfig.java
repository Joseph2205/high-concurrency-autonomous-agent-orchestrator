package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Binds Resilience4j primitives to Micrometer so that circuit-breaker,
 * bulkhead, retry and time-limiter state appear under /actuator/metrics and
 * /actuator/prometheus.
 */
@Configuration
public class ResilienceMetricsConfig {

    private final ResilienceRegistry registry;
    private final MeterRegistry meterRegistry;

    public ResilienceMetricsConfig(ResilienceRegistry registry,
                                   MeterRegistry meterRegistry) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void bindMetrics() {
        TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(registry.circuitBreakerRegistry())
                .bindTo(meterRegistry);
        TaggedBulkheadMetrics
                .ofBulkheadRegistry(registry.bulkheadRegistry())
                .bindTo(meterRegistry);
        TaggedRetryMetrics
                .ofRetryRegistry(registry.retryRegistry())
                .bindTo(meterRegistry);
        TaggedTimeLimiterMetrics
                .ofTimeLimiterRegistry(registry.timeLimiterRegistry())
                .bindTo(meterRegistry);
    }
}
