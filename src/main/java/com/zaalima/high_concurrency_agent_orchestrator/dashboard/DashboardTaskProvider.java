package com.zaalima.high_concurrency_agent_orchestrator.dashboard;

import com.zaalima.high_concurrency_agent_orchestrator.AgentTask;
import com.zaalima.high_concurrency_agent_orchestrator.AgentTaskService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DashboardTaskProvider {

    private final AgentTaskService taskService;

    public DashboardTaskProvider(AgentTaskService taskService) {
        this.taskService = taskService;
    }

    public DashboardSnapshot.TaskCounts taskCounts() {
        List<AgentTask> tasks = taskService.getAllTasks();

        long running = tasks.stream()
                .filter(t -> "RUNNING".equals(t.getStatus()))
                .count();
        long completed = tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .count();
        long failed = tasks.stream()
                .filter(t -> "FAILED".equals(t.getStatus()))
                .count();

        return new DashboardSnapshot.TaskCounts(running, completed, failed);
    }

    public List<DashboardSnapshot.TaskView> recentTasks(int limit) {
        return taskService.getAllTasks().stream()
                .filter(t -> isValidUuid(t.getTaskId()))
                .sorted(Comparator.comparing(AgentTask::getTaskId).reversed())
                .limit(limit)
                .map(t -> new DashboardSnapshot.TaskView(
                        t.getTaskId(),
                        t.getTaskName(),
                        t.getStatus(),
                        true))
                .toList();
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
