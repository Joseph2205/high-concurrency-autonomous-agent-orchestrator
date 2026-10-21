package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeoutException;

@Component
public class ResilienceRegistry {

    public static final String EXTERNAL_CALL = "externalApi";

    private static final Logger logger =
            LoggerFactory.getLogger(ResilienceRegistry.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public ResilienceRegistry(ResilienceProperties properties) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.failureRateThreshold())
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(
                        properties.permittedCallsInHalfOpenState())
                .waitDurationInOpenState(properties.waitDurationInOpenState())
                .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        BulkheadConfig bhConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(properties.maxBulkheadConcurrentCalls())
                .maxWaitDuration(properties.maxBulkheadWait())
                .build();
        this.bulkheadRegistry = BulkheadRegistry.of(bhConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(properties.maxRetryAttempts())
                .waitDuration(properties.retryWaitDuration())
                .retryExceptions(RuntimeException.class)
                .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);

        TimeLimiterConfig tlConfig = TimeLimiterConfig.custom()
                .timeoutDuration(properties.callTimeout())
                .cancelRunningFuture(true)
                .build();
        this.timeLimiterRegistry = TimeLimiterRegistry.of(tlConfig);

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(EXTERNAL_CALL);
        this.bulkhead = bulkheadRegistry.bulkhead(EXTERNAL_CALL);
        this.retry = retryRegistry.retry(EXTERNAL_CALL);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(EXTERNAL_CALL);

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> logger.warn(
                        "Circuit breaker {} state: {} -> {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }

    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }

    public BulkheadRegistry bulkheadRegistry() {
        return bulkheadRegistry;
    }

    public RetryRegistry retryRegistry() {
        return retryRegistry;
    }

    public TimeLimiterRegistry timeLimiterRegistry() {
        return timeLimiterRegistry;
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
