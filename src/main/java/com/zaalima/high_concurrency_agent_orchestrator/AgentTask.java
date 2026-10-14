package com.zaalima.high_concurrency_agent_orchestrator;

public class AgentTask {

    private String taskId;
    private String taskName;
    private String status;
    private String result;

    public AgentTask(){
    }

    public AgentTask(String taskId, String taskName, String status, String result){
        this.taskId = taskId;
        this.taskName = taskName;
        this.status = status;
        this.result = result;
    }

    public String getTaskId(){
        return taskId;
    }

    public void setTaskId(String taskId){
        this.taskId = taskId;
    }

    public String getTaskName(){
        return taskName;
    }

    public void setTaskName(String taskName){
        this.taskName = taskName;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getResult(){
        return result;
    }

    public void setResult(String result){
        this.result = result;
    }
}
