
package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    public AgentTaskController(AgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentTask submitTask(@RequestBody TaskRequest request) {

        if (request.taskName() == null
                || request.taskName().isBlank()) {
            throw new IllegalArgumentException(
                    "Task name must not be empty"
            );
        }

        return agentTaskService.submitTask(request.taskName());
    }

    @GetMapping("/{taskId}")
    public AgentTask getTask(@PathVariable String taskId) {

        AgentTask task = agentTaskService.getTask(taskId);

        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }

        return task;
    }

    public record TaskRequest(String taskName) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class TaskNotFoundException
            extends RuntimeException {

        public TaskNotFoundException(String taskId) {
            super("Task not found: " + taskId);
        }
    }
}
