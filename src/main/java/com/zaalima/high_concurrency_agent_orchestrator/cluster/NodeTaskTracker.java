package com.zaalima.high_concurrency_agent_orchestrator.cluster;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class NodeTaskTracker {

    private final AtomicLong activeTasks = new AtomicLong(0);

    /**
     * Called when a node starts executing a task.
     */
    public void taskStarted() {

        activeTasks.incrementAndGet();
    }

    /**
     * Called when a node finishes executing a task.
     */
    public void taskCompleted() {

        activeTasks.updateAndGet(current -> Math.max(0, current - 1));
    }

    /**
     * Returns the number of currently executing tasks on this node.
     */
    public long getActiveTasks() {

        return activeTasks.get();
    }
}