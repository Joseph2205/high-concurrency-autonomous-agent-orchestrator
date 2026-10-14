
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
        return agentTaskService.submitTask(request.taskName());
    }

    public record TaskRequest(String taskName) {
    }
}
