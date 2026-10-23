package com.zaalima.high_concurrency_agent_orchestrator;

import org.springframework.scheduling.config.Task;

import java.util.concurrent.atomic.AtomicReference;

public class AgentTask {

    private final String taskId;
    private final String taskName;

    private record TaskState(String status, String result){}

    private final AtomicReference <TaskState> state;



    public AgentTask(
            String taskId,
            String taskName,
            String status,
            String result){

        this.taskId = taskId;
        this.taskName = taskName;
        this.state = new AtomicReference<>(
                new TaskState(status, result)
        );
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getStatus() {
        return state.get().status();
    }

    public String getResult(){
        return state.get().result();
    }

    public void setStatus(String newStatus){
        state.updateAndGet(current ->
                new TaskState(newStatus, current.result())
        );
    }

    public void setResult(String newResult){
        state.updateAndGet(current ->
                new TaskState(current.status(), newResult)
        );
    }


    public boolean transitionStatus(
            String expectedStatus,
            String newStatus) {

        while (true) {
            TaskState current = state.get();

            if (!java.util.Objects.equals(
                    current.status(), expectedStatus)) {
                return false;
            }

            TaskState updated = new TaskState(
                    newStatus,
                    current.result()
            );

            if (state.compareAndSet(current, updated)) {
                return true;
            }
        }
    }


   public boolean transition(
           String expectedStatus,
           String newStatus,
           String newResult){

        while (true){
            TaskState current = state.get();

            if (!java.util.Objects.equals(
                    current.status(), expectedStatus)){
                return false;
            }

            TaskState updated = new TaskState(
                    newStatus,
                    newResult
            );

            if (state.compareAndSet(current, updated)){
                return true;
            }
        }
   }
}
