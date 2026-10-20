# gRPC Service Description — AgentRegistrationService

Owner: Member 3
Port: `orchestrator.grpc.port` (default `9090`)
Lifecycle: started and stopped with the Spring Boot application
(`GrpcServerLifecycle`, `SmartLifecycle`).

## RPCs

| RPC               | Request              | Response               | Behaviour |
|-------------------|----------------------|------------------------|-----------|
| `RegisterAgent`   | `RegisterAgentRequest` | `RegisterAgentResponse` | Validates `agent_id` and `agent_name`, stores the agent in the registry, returns the accepting node id. |
| `Heartbeat`       | `HeartbeatRequest`     | `HeartbeatResponse`     | Refreshes last-seen time and active-task load. `alive=false` when the agent is unknown. |
| `DeregisterAgent` | `DeregisterAgentRequest` | `DeregisterAgentResponse` | Removes the agent. `removed=false` when it was not registered. |

## Error codes

| Condition                 | gRPC Status         |
|---------------------------|---------------------|
| Empty `agent_id`          | `INVALID_ARGUMENT`  |
| Empty `agent_name`        | `INVALID_ARGUMENT`  |
| Unexpected server failure | `INTERNAL`          |

## Wiring

- `AgentRegistrationGrpcService` extends the generated
  `AgentRegistrationServiceGrpc.AgentRegistrationServiceImplBase`.
- All state lives in `AgentRegistryService`, which is shared with the REST
  layer, so both transports always see the same registry.

## Running

The server starts automatically with the application. To disable it (e.g. in
tests):

```properties
orchestrator.grpc.enabled=false
```
