
package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.List;

@Service
public class AgentTaskService {

    private final ExecutorService virtualThreadExecutor;

    private final ConcurrentMap<String, AgentTask> tasks =
            new ConcurrentHashMap<>();

    public AgentTaskService(ExecutorService virtualThreadExecutor) {
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public AgentTask submitTask(String taskName) {

        String taskId = UUID.randomUUID().toString();

        AgentTask task = new AgentTask(
                taskId,
                taskName,
                "RUNNING",
                null
        );

        tasks.put(taskId, task);

        try {
            virtualThreadExecutor.submit(() -> {
                try {
                    // Simulate an I/O- bound agent operation
                    Thread.sleep(2000);

                    task.setResult("Task completed successfully");
                    task.setStatus("COMPLETED");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    task.setResult("Task interrupted");
                    task.setStatus("FAILED");

                } catch (Exception e) {
                    task.setResult("Task execution failed");
                    task.setStatus("FAILED");
                }
            });
        } catch (RuntimeException e) {
            task.setResult("Task could not be submitted");
            task.setStatus("FAILED");
        }

        return task;
    }

    public AgentTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<AgentTask> getAllTasks() {
        return List.copyOf(tasks.values());
    }
}
