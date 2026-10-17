package com.zaalima.high_concurrency_agent_orchestrator;

import java.util.concurrent.atomic.AtomicReference;

public class AgentTask {

    private final String taskId;
    private final String taskName;

    private final AtomicReference <String> status =
            new AtomicReference<>("RUNNING");

    private final AtomicReference <String> result =
            new AtomicReference<>(null);


    public AgentTask(
            String taskId,
            String taskName,
            String status,
            String result){

        this.taskId = taskId;
        this.taskName = taskName;
        this.status.set(status);
        this.result.set(result);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public String getResult() {
        return result.get();
    }

    public void setResult(String result) {
        this.result.set(result);
    }
}
