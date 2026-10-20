# Payload Submission Flow (Agent -> Orchestrator)

Owner: Member 3 (Day 7). Downstream executor owned by Member 2.

## Sequence

```
Agent                         Orchestrator                    Virtual-thread executor
  |                                |                                  |
  |-- POST /api/agents/payload --->|                                  |
  |   (or gRPC SubmitPayload)      |                                  |
  |                                |-- registry.find(agentId)         |
  |                                |   (404 / NOT_FOUND if unknown)   |
  |                                |-- taskService.submitTask(...) --->|
  |                                |        returns taskId RUNNING    |-- sleep / I/O
  |<-- 202 Accepted {taskId} ------|                                  |
  |                                |<-- task COMPLETED (async) -------|
  |-- GET /api/tasks/{taskId} ---->|                                  |
  |<-- {status: COMPLETED} --------|                                  |
```

## REST

`POST /api/agents/payload` → `202 Accepted`

```json
{
  "agentId": "agent-001",
  "taskName": "fetch-analytics",
  "payload": { "userId": "42", "range": "7d" }
}
```

Response:

```json
{
  "accepted": true,
  "taskId": "<uuid>",
  "status": "RUNNING",
  "message": "Payload accepted and queued"
}
```

## gRPC

`AgentRegistrationService.SubmitPayload(SubmitPayloadRequest) → SubmitPayloadResponse`
with the same fields. Unknown agent → `NOT_FOUND`.

## Rules

- An agent must be registered first; unregistered payloads are rejected with 404
  (REST) / `NOT_FOUND` (gRPC).
- The task itself is created through Member 2's `AgentTaskService`, so it runs
  on a Java 21 virtual thread and is queryable at `/api/tasks/{taskId}`.
- Payload key/value pairs are appended to the task name for traceability in the
  current milestone; a structured payload store is a later refinement.
