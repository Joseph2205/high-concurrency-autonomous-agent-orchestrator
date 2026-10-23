
package com.zaalima.high_concurrency_agent_orchestrator;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AgentTaskServiceTest {

    @Test
    void submitTaskShouldCompleteSuccessfully() throws Exception {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            AgentTask task = service.submitTask("Test task");

            assertNotNull(task.getTaskId());
            assertEquals("Test task", task.getTaskName());

            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(5);

            while (!"COMPLETED".equals(task.getStatus())
                    && !"FAILED".equals(task.getStatus())
                    && System.nanoTime() < deadline) {
                Thread.sleep(50);
            }

            assertEquals("COMPLETED", task.getStatus());
            assertEquals(
                    "Task completed successfully",
                    task.getResult()
            );
        }
    }

    @Test
    void submittedTaskShouldAppearInTaskList() {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            AgentTask task = service.submitTask("List test");

            assertTrue(
                    service.getAllTasks().stream()
                            .anyMatch(t ->
                                    t.getTaskId().equals(task.getTaskId()))
            );
        }
    }

    @Test
    void unknownTaskIdShouldReturnNull() {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            assertNull(service.getTask("unknown-task-id"));
        }
    }

    @Test
    void runningTaskShouldBeCancelled() throws Exception {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            AgentTask task = service.submitTask("Cancellation test");

            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(2);

            while (!"RUNNING".equals(task.getStatus())
                    && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            boolean cancelled = service.cancelTask(task.getTaskId());

            assertTrue(cancelled);
            assertEquals("CANCELLED", task.getStatus());
            assertEquals("Task cancelled by request", task.getResult());
        }
    }

    @Test
    void unknownTaskCannotBeCancelled() {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            assertFalse(service.cancelTask("unknown-task-id"));
        }
    }

    @Test
    void completedTaskCannotBeCancelled() throws Exception {
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            AgentTaskService service = new AgentTaskService(executor);

            AgentTask task = service.submitTask("Completion test");

            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(5);

            while (!"COMPLETED".equals(task.getStatus())
                    && !"FAILED".equals(task.getStatus())
                    && System.nanoTime() < deadline) {
                Thread.sleep(50);
            }

            assertEquals("COMPLETED", task.getStatus());
            assertFalse(service.cancelTask(task.getTaskId()));
        }
    }

    @Test
    void cancelledTaskShouldNotTransitionToCompleted(){
        AgentTask task = new AgentTask(
                "test-id",
                "Cancellation test",
                "RUNNING",
                null
        );

        assertTrue(task.transitionStatus("RUNNING", "CANCELLED"));
        assertFalse(task.transitionStatus("RUNNING", "COMPLETED"));
        assertEquals("CANCELLED", task.getStatus());
    }

    @Test
    void cancelledTaskShouldKeepConsistentStatusAndResult() throws Exception{

        try (ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor()){

            AgentTaskService service = new AgentTaskService(executor);

            AgentTask task = service.submitTask("Consistency test");

            boolean cancelled = service.cancelTask(task.getTaskId());

            assertTrue(cancelled);

            assertEquals("CANCELLED", task.getStatus());
            assertEquals("Task cancelled by request", task.getResult());

//            A second cancellation should not change the final state.
            assertFalse(service.cancelTask(task.getTaskId()));

            assertEquals("CANCELLED", task.getStatus());
            assertEquals("Task cancelled by request", task.getResult());
        }
    }
}
