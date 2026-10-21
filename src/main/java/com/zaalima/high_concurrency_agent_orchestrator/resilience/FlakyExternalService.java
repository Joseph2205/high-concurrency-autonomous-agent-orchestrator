package com.zaalima.high_concurrency_agent_orchestrator.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configurable fake upstream used to inject slow / erroring / disconnecting
 * behaviour into resilience tests. Never registered as a bean; tests build it.
 */
@Component
public class FlakyExternalService {

    public enum Mode {
        OK,
        SLOW,
        HTTP_500,
        DISCONNECT
    }

    private final AtomicInteger invocations = new AtomicInteger();
    private volatile Mode mode = Mode.OK;

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public int invocations() {
        return invocations.get();
    }

    public void reset() {
        invocations.set(0);
        mode = Mode.OK;
    }

    public String invoke(long simulatedLatencyMs) {
        invocations.incrementAndGet();

        switch (mode) {
            case SLOW -> sleep(simulatedLatencyMs);
            case HTTP_500 -> throw new UpstreamServerException("upstream returned 500");
            case DISCONNECT -> throw new UpstreamConnectionException("connection reset by peer");
            default -> sleep(Math.min(simulatedLatencyMs, 5));
        }
        return "upstream-ok";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class UpstreamServerException extends RuntimeException {
        public UpstreamServerException(String message) {
            super(message);
        }
    }

    public static class UpstreamConnectionException extends RuntimeException {
        public UpstreamConnectionException(String message) {
            super(message);
        }
    }
}
