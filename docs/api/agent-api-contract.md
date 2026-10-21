# Agent Registration API Contract (Draft)

Owner: Member 3 (gRPC / REST interfaces)
Status: Draft — Day 1 deliverable
Version: v1

## 1. Overview

Autonomous agents register with the orchestrator before submitting work.
The same contract is exposed over two transports:

- REST/JSON — `http://<host>:8080/api/agents`
- gRPC — `AgentRegistrationService` on the orchestrator gRPC port (default `9090`)

Both transports share identical fields and semantics, defined once in
`proto/agent.proto`.

## 2. REST endpoints (draft)

| Method | Path                   | Purpose                          |
|--------|------------------------|----------------------------------|
| POST   | `/api/agents`          | Register an agent                |
| POST   | `/api/agents/heartbeat`| Refresh liveness, report load    |
| DELETE | `/api/agents/{agentId}`| Deregister an agent              |
| GET    | `/api/agents`          | List registered agents           |

## 3. gRPC contract (proto/agent.proto)

```proto
service AgentRegistrationService {
  rpc RegisterAgent (RegisterAgentRequest) returns (RegisterAgentResponse);
  rpc Heartbeat (HeartbeatRequest) returns (HeartbeatResponse);
  rpc DeregisterAgent (DeregisterAgentRequest) returns (DeregisterAgentResponse);
}
```

### RegisterAgentRequest

| Field      | Type   | Rule                  |
|------------|--------|-----------------------|
| agent_id   | string | required, unique      |
| agent_name | string | required              |
| endpoint   | string | optional callback URL |
| metadata   | map    | optional key/value    |

### RegisterAgentResponse

| Field    | Type   | Meaning                              |
|----------|--------|--------------------------------------|
| accepted | bool   | `true` when registration succeeded   |
| message  | string | human-readable result                |
| node_id  | string | orchestrator node that accepted      |

### HeartbeatRequest / HeartbeatResponse

| Request field   | Type  | Meaning                       |
|-----------------|-------|-------------------------------|
| agent_id        | string| required                      |
| active_tasks    | int64 | load currently handled        |

| Response field          | Type  | Meaning                      |
|-------------------------|-------|------------------------------|
| alive                   | bool  | agent still known            |
| server_time_epoch_ms    | int64 | orchestrator clock           |

`alive=false` means the agent was unknown (expired or never registered)
and must re-register.

### DeregisterAgentRequest / DeregisterAgentResponse

| Request field | Type   |
|---------------|--------|
| agent_id      | string |

| Response field | Type   | Meaning                 |
|----------------|--------|-------------------------|
| removed        | bool   | agent was known & removed|

## 4. REST DTO mapping

REST DTOs in `com.zaalima.high_concurrency_agent_orchestrator.dto`
mirror the proto messages one-to-one:

- `AgentRegistrationRequest` / `AgentRegistrationResponse`
- `AgentHeartbeatRequest` / `AgentHeartbeatResponse`
- `AgentDeregisterRequest` / `AgentDeregisterResponse`

## 5. Error handling

- Missing `agent_id` / `agent_name` → HTTP 400 (`INVALID_ARGUMENT` in gRPC)
- Heartbeat for unknown agent → HTTP 404 with `alive=false` semantics
- Server faults → HTTP 500 (`INTERNAL` in gRPC)

## 6. Compatibility rules

Any change to this contract must be:

1. backward compatible (never reuse field numbers in proto),
2. mirrored in REST DTOs in the same commit,
3. documented in `docs/openapi/agent-api.yaml` (from Day 3 onwards).
