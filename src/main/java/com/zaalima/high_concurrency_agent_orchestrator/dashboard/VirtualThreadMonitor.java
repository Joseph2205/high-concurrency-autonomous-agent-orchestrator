package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reports live virtual-thread state.
 *
 * <p>Virtual threads are not enumerable through {@link ThreadMXBean}, so the
 * executor used by the orchestrator is wrapped in a counting factory that keeps
 * a live gauge of started-minus-finished virtual threads. Platform-thread
 * counters come from the JVM's thread MX bean.
 */
@Component
public class VirtualThreadMonitor {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final AtomicLong liveVirtualThreads = new AtomicLong();

    public VirtualThreadStats sample(ExecutorService executor) {
        boolean virtualThreadExecutor = executor instanceof VirtualThreadExecutor;

        return new VirtualThreadStats(
                liveVirtualThreads.get(),
                threadMXBean.getThreadCount(),
                threadMXBean.getPeakThreadCount(),
                virtualThreadExecutor);
    }

    /** Marker for executors that run tasks on Java 21 virtual threads. */
    public interface VirtualThreadExecutor {
    }

    public void onVirtualThreadStarted() {
        liveVirtualThreads.incrementAndGet();
    }

    public void onVirtualThreadFinished() {
        liveVirtualThreads.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    public long liveVirtualThreads() {
        return liveVirtualThreads.get();
    }

    public record VirtualThreadStats(
            long liveVirtualThreads,
            int platformThreadCount,
            int peakPlatformThreadCount,
            boolean virtualThreadExecutor) {
    }
}
