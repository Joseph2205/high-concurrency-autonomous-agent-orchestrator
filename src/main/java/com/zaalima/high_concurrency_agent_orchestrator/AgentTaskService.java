
package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AgentTaskService {

    private static final Logger logger =
            LoggerFactory.getLogger(AgentTaskService.class);

    private final ExecutorService virtualThreadExecutor;

    private final ConcurrentMap<String, Future<?>> runningTasks =
            new ConcurrentHashMap<>();

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
            Future<?> future = virtualThreadExecutor.submit(() -> {
                try {
                    logger.info(
                            "Executing task {} on virtual thread: {}",
                            taskName,
                            Thread.currentThread().isVirtual()
                    );

                    // Simulate an I/O- bound agent operation
                    Thread.sleep(2000);

                    if (task.transition(
                            "RUNNING",
                            "COMPLETED",
                            "Task completed successfully")){

                        logger.info(
                                "Task completed successfully. Task ID: {}",
                                taskId
                        );
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    boolean markedFailed = task.transition(
                            "RUNNING",
                            "FAILED",
                            "Task interrupted"
                    );

                    if (markedFailed) {
                        logger.warn(
                                "Task interrupted. Task ID: {}",
                                taskId
                        );
                    }


                } catch (Exception e) {
                    boolean markedFailed = task.transition(
                            "RUNNING",
                            "FAILED",
                            "Task execution failed"
                    );

                    logger.error(
                            "Task execution failed. Task ID: {}",
                            taskId,
                            e
                    );
                }finally {
                    runningTasks.remove(taskId);
                }
            });

            runningTasks.put(taskId, future);

            if (future.isDone()){
                runningTasks.remove(taskId, future);
            }

        } catch (RuntimeException e) {
            task.transition(
                    "RUNNING",
                    "FAILED",
                    "Task could not be submitted"
            );

            logger.error(
                    "Task submission failed. Task ID: {}",
                    taskId,
                    e
            );
        }

        return task;
    }

    public boolean cancelTask(String taskId){
        AgentTask task = tasks.get(taskId);

        if (task == null){
            logger.warn("Cannot cancel task because it was not found: {}", taskId);
            return false;
        }

        Future<?> future = runningTasks.get(taskId);

        if(future == null){
            logger.info(
                    "Task  is no longer running. Task ID: {}",
                    taskId
            );
            return false;
        }


        boolean markedCancelled = task.transition(
                "RUNNING",
                "CANCELLED",
                "Task cancelled by request"
        );

        if(!markedCancelled){

            logger.info(
                    "Task cannot be cancelled because its status is {}. Task ID: {}",
                    task.getStatus(),
                    taskId);

            return false;
        }

        boolean cancellationRequested = future.cancel(true);

        if (cancellationRequested){
            logger.info("Task cancellation requested. Task ID: {}", taskId);
        }else {
            logger.warn(
                    "Task status was marked cancelled, but execution cancellation was not accepted. Task ID: {}",
                    taskId
            );
        }

        return cancellationRequested;

    }



    public AgentTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<AgentTask> getAllTasks() {
        return List.copyOf(tasks.values());
    }
}
