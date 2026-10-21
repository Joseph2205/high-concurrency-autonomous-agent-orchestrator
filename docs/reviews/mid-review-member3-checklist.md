# Mid-Review Checklist (Member 3) + Interface Section for Peer Review

Owner: Member 3
Project: High-Concurrency Autonomous Agent Orchestrator
Week 2 milestone (end of Week 2)

## 1. Member 3 mid-review checklist

| Item | Status | Evidence |
|------|--------|----------|
| gRPC `AgentRegistrationService` implemented (register/heartbeat/deregister) | Done | `grpc/AgentRegistrationGrpcService.java`, `AgentRegistrationGrpcIntegrationTest` |
| Equivalent REST controllers | Done | `controller/AgentRegistrationController.java`, OpenAPI in `docs/openapi/agent-api.yaml` |
| Secure registration (shared token) | Done | `security/RegistrationTokenFilter.java`, `grpc/RegistrationTokenInterceptor.java` |
| Agent registry synced with Redis | Done | `agent/RedisAgentRegistry.java` (`orchestrator:agents` hash) |
| Payload submission (gRPC + REST) into orchestrator | Done | `controller/AgentPayloadController.java`, `SubmitPayload` RPC |
| Fake-agent simulator | Done | `simulator/FakeAgentSimulator.java` |
| 10k agents tested with Member 2 | Done | `load/TenThousandAgentLoadTest` → 10,000 registered, 0 failures |
| Integration tests + Postman collection | Done | `test/.../controller`, `test/.../grpc`, `test/.../security`, `docs/postman/` |

## 2. Interface section (for the peer-review pack)

### REST surface owned by Member 3

| Method | Path                    | Auth         | Purpose |
|--------|-------------------------|--------------|---------|
| POST   | `/api/agents`           | token header | register |
| POST   | `/api/agents/heartbeat` | token header | liveness + load |
| POST   | `/api/agents/payload`   | token header | submit work |
| DELETE | `/api/agents/{agentId}` | token header | deregister |
| GET    | `/api/agents`           | token header | list |

### gRPC surface owned by Member 3

`AgentRegistrationService`: `RegisterAgent`, `Heartbeat`, `DeregisterAgent`,
`SubmitPayload` (metadata key `registration-token` when secured).

### Boundaries with other members

- **Member 1 (Redis):** Member 3 reads/writes only the `orchestrator:agents`
  hash. Locks (`orchestrator:lock:*`) and Pub/Sub channels belong to Member 1.
- **Member 2 (executor):** payload submission calls `AgentTaskService.submitTask`,
  which owns the virtual-thread execution and task lifecycle. Member 3 does not
  touch `AgentTask` internals beyond reading status.
- **Member 4 (CI/tests):** the token filter and gRPC interceptor are no-ops when
  no token is configured, so CI and Testcontainers runs need no extra setup.

### Contracts the mid-review must see working

1. Register an agent over gRPC and read it back over REST (shared registry).
2. Reject registration without the token.
3. Submit a payload and observe a RUNNING task on a virtual thread.
4. 10,000-agent registration burst with zero failures.

## 3. Demo script (2 minutes)

1. Start the app (`./mvnw spring-boot:run`).
2. `POST /api/agents` → 201.
3. `POST /api/agents/payload` → 202 with taskId.
4. `GET /api/tasks/{taskId}` → COMPLETED.
5. Enable the token, repeat step 2 → 401.
6. Show `grpc` port open on 9090 and the integration test report.
