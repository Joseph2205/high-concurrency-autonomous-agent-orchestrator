package com.zaalima.high_concurrency_agent_orchestrator;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AgentTaskServiceTest {

    @Test
    void submitTaskShouldCompleteSuccessfully() throws Exception{
        try (ExecutorService executer =
                     Executors.newVirtualThreadPerTaskExecutor()){

            AgentTaskService service = new AgentTaskService(executer);

            AgentTask task = service.submitTask("Test task");

            assertNotNull(task.getTaskId());
            assertEquals("Test task",task.getTaskName());

            long deadline = System.nanoTime()
                    +TimeUnit.SECONDS.toNanos(5);

            while (!"COMPLETED".equals(task.getStatus())
            && !"FAILED".equals(task.getStatus())
            && System.nanoTime() < deadline){
                Thread.sleep(50);
            }

            assertEquals("COMPLETED", task.getStatus());
            assertNotNull(task.getResult());
            assertEquals(
                    "Task completed successfully",
                    task.getResult().trim()
            );

        }
    }

    @Test
    void submittedTaskShouldAppearInTaskList(){
        try (ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor()){

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
    void unknownTaskIdShouldReturnNull(){
        try (ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor()){

            AgentTaskService service = new AgentTaskService(executor);

            assertNull(service.getTask("unknown-task-id"));

        }
    }
}
