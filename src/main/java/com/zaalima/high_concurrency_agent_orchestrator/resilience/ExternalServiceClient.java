package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Component
public class ExternalServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(ExternalServiceClient.class);

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ExecutorService externalCallExecutor;

    public ExternalServiceClient(ResilienceRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker();
        this.bulkhead = registry.bulkhead();
        this.retry = registry.retry();
        this.timeLimiter = registry.timeLimiter();
        this.externalCallExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public <T> T call(Supplier<T> externalCall, Supplier<T> fallback) {
        Supplier<T> guarded = decorate(externalCall);
        try {
            return guarded.get();
        } catch (RuntimeException e) {
            if (fallback != null) {
                logger.warn("External call failed ({}), applying fallback",
                        rootCause(e).getClass().getSimpleName());
                return fallback.get();
            }
            throw e;
        }
    }

    private <T> Supplier<T> decorate(Supplier<T> externalCall) {
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, externalCall);
        decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
        final Supplier<T> retried = Retry.decorateSupplier(retry, decorated);
        return () -> {
            try {
                return timeLimiter.executeFutureSupplier(
                        () -> CompletableFuture.supplyAsync(retried, externalCallExecutor));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
