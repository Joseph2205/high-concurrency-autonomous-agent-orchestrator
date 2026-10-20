package com.zaalima.high_concurrency_agent_orchestrator;

import com.zaalima.high_concurrency_agent_orchestrator.dashboard.VirtualThreadMonitor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configuration
public class VirtualThreadConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutor(VirtualThreadMonitor monitor) {
        ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
        return new CountingExecutorService(delegate, monitor);
    }

    /**
     * Delegating {@link ExecutorService} that reports virtual-thread starts and
     * finishes to the {@link VirtualThreadMonitor} so the dashboard can show a
     * live virtual-thread gauge.
     */
    static final class CountingExecutorService
            implements ExecutorService, VirtualThreadMonitor.VirtualThreadExecutor {

        private final ExecutorService delegate;
        private final VirtualThreadMonitor monitor;

        CountingExecutorService(ExecutorService delegate, VirtualThreadMonitor monitor) {
            this.delegate = delegate;
            this.monitor = monitor;
        }

        private Runnable count(Runnable task) {
            return () -> {
                monitor.onVirtualThreadStarted();
                try {
                    task.run();
                } finally {
                    monitor.onVirtualThreadFinished();
                }
            };
        }

        private <T> Callable<T> count(Callable<T> task) {
            return () -> {
                monitor.onVirtualThreadStarted();
                try {
                    return task.call();
                } finally {
                    monitor.onVirtualThreadFinished();
                }
            };
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(count(command));
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(count(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(count(task), result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(count(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return delegate.invokeAll(tasks.stream().map(this::count).toList());
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks,
                long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks.stream().map(this::count).toList(), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks.stream().map(this::count).toList());
        }

        @Override
        public <T> T invokeAny(
                Collection<? extends Callable<T>> tasks,
                long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks.stream().map(this::count).toList(), timeout, unit);
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
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
