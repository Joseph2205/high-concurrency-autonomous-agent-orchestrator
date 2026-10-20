package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class ResilienceRegistry {

    public static final String EXTERNAL_CALL = "externalApi";

    private static final Logger logger =
            LoggerFactory.getLogger(ResilienceRegistry.class);

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public ResilienceRegistry(ResilienceProperties properties) {
        this.circuitBreaker = CircuitBreaker.of(
                EXTERNAL_CALL,
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(properties.failureRateThreshold())
                        .slidingWindowSize(properties.slidingWindowSize())
                        .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                        .permittedNumberOfCallsInHalfOpenState(
                                properties.permittedCallsInHalfOpenState())
                        .waitDurationInOpenState(properties.waitDurationInOpenState())
                        .build());

        this.bulkhead = Bulkhead.of(
                EXTERNAL_CALL,
                BulkheadConfig.custom()
                        .maxConcurrentCalls(properties.maxBulkheadConcurrentCalls())
                        .maxWaitDuration(properties.maxBulkheadWait())
                        .build());

        this.retry = Retry.of(
                EXTERNAL_CALL,
                RetryConfig.custom()
                        .maxAttempts(properties.maxRetryAttempts())
                        .waitDuration(properties.retryWaitDuration())
                        .retryExceptions(RuntimeException.class)
                        .build());

        this.timeLimiter = TimeLimiter.of(
                EXTERNAL_CALL,
                TimeLimiterConfig.custom()
                        .timeoutDuration(properties.callTimeout())
                        .cancelRunningFuture(true)
                        .build());

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> logger.warn(
                        "Circuit breaker {} state: {} -> {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }

    public CircuitBreaker circuitBreaker() {
        return circuitBreaker;
    }

    public Bulkhead bulkhead() {
        return bulkhead;
    }

    public Retry retry() {
        return retry;
    }

    public TimeLimiter timeLimiter() {
        return timeLimiter;
    }

    public boolean isTimeout(Throwable throwable) {
        return throwable instanceof TimeoutException;
    }
}
