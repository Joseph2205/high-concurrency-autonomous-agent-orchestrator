package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "orchestrator.resilience")
public record ResilienceProperties(
        float failureRateThreshold,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        int permittedCallsInHalfOpenState,
        Duration waitDurationInOpenState,
        int maxBulkheadConcurrentCalls,
        Duration maxBulkheadWait,
        int maxRetryAttempts,
        Duration retryWaitDuration,
        Duration callTimeout) {

    public ResilienceProperties {
        if (failureRateThreshold <= 0) {
            failureRateThreshold = 50f;
        }
        if (slidingWindowSize <= 0) {
            slidingWindowSize = 10;
        }
        if (minimumNumberOfCalls <= 0) {
            minimumNumberOfCalls = 5;
        }
        if (permittedCallsInHalfOpenState <= 0) {
            permittedCallsInHalfOpenState = 3;
        }
        if (waitDurationInOpenState == null) {
            waitDurationInOpenState = Duration.ofSeconds(10);
        }
        if (maxBulkheadConcurrentCalls <= 0) {
            maxBulkheadConcurrentCalls = 50;
        }
        if (maxBulkheadWait == null) {
            maxBulkheadWait = Duration.ofMillis(500);
        }
        if (maxRetryAttempts <= 0) {
            maxRetryAttempts = 2;
        }
        if (retryWaitDuration == null) {
            retryWaitDuration = Duration.ofMillis(200);
        }
        if (callTimeout == null) {
            callTimeout = Duration.ofSeconds(2);
        }
    }
}
