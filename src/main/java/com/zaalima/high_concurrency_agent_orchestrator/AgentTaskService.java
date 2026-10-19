
package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AgentTaskService {

    private static final Logger logger =
            LoggerFactory.getLogger(AgentTaskService.class);

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
                    logger.info(
                            "Executing task {} on virtual thread: {}",
                            taskName,
                            Thread.currentThread().isVirtual()
                    );

                    // Simulate an I/O- bound agent operation
                    Thread.sleep(2000);

                    task.setResult("Task completed successfully");
                    task.setStatus("COMPLETED");

                    logger.info("Task completed successfully. Task ID: {}", taskId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    task.setResult("Task interrupted");
                    task.setStatus("FAILED");

                    logger.warn("Task was interrupted. Task ID: {}", taskId);

                } catch (Exception e) {
                    task.setResult("Task execution failed");
                    task.setStatus("FAILED");

                    logger.error(
                            "Task execution failed. Task ID: {}",
                            taskId,
                            e
                    );
                }
            });
        } catch (RuntimeException e) {
            task.setResult("Task could not be submitted");
            task.setStatus("FAILED");

            logger.error(
                    "Task submission failed. Task ID: {}",
                    taskId,
                    e
            );
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
