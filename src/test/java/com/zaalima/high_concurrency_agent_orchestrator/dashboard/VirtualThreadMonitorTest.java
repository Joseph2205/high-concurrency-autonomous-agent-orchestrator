package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualThreadMonitorTest {

    @Test
    void monitorShouldTrackLiveVirtualThreads() throws Exception {
        VirtualThreadMonitor monitor = new VirtualThreadMonitor();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService counting = new CountingExecutor(executor, monitor);

        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            counting.submit(() -> {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertEquals(3, monitor.liveVirtualThreads());

        release.countDown();
        Thread.sleep(200);
        assertEquals(0, monitor.liveVirtualThreads());

        executor.shutdownNow();
    }

    private static final class CountingExecutor extends java.util.concurrent
            .AbstractExecutorService {
        private final ExecutorService delegate;
        private final VirtualThreadMonitor monitor;

        CountingExecutor(ExecutorService delegate, VirtualThreadMonitor monitor) {
            this.delegate = delegate;
            this.monitor = monitor;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(() -> {
                monitor.onVirtualThreadStarted();
                try {
                    command.run();
                } finally {
                    monitor.onVirtualThreadFinished();
                }
            });
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }
}
