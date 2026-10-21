# High-Concurrency Autonomous Agent Orchestrator

## Project Overview

The High-Concurrency Autonomous Agent Orchestrator is a Java-based system designed to coordinate and execute large numbers of concurrent autonomous agent workflows efficiently.

The project uses Java 21 Virtual Threads and Spring Boot to explore high-concurrency task execution, distributed coordination, communication interfaces, resilience, and performance benchmarking.

## Objectives

* Build a high-concurrency orchestration backend.
* Explore Java 21 Virtual Threads and Structured Concurrency.
* Implement Redis-based communication and distributed locking.
* Provide REST and gRPC interfaces for agent registration and communication.
* Configure containerization and automated testing.
* Benchmark performance under concurrent workloads.

## Technology Stack

* Java 21
* Spring Boot 3
* Maven
* Redis
* REST APIs
* gRPC
* Docker and Docker Compose
* Git and GitHub Actions
* JUnit and Testcontainers

## Team Development

The project is developed collaboratively by four team members.

Each member works on an assigned module and contributes through feature branches and pull requests.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines and [docs/work-distribution.md](docs/work-distribution.md) for team responsibilities.

## Project Status

Under development.

## Member 3 — Interfaces, Resilience & Dashboard

This branch (`feature/member-3-grpc-agent`) delivers the agent-facing
interfaces, resilience layer, and the terminal/HTTP dashboard.

| Area | Deliverable | Docs |
|------|-------------|------|
| Protocols | `proto/agent.proto` (RegisterAgent, Heartbeat, DeregisterAgent, SubmitPayload) | [docs/api/agent-api-contract.md](docs/api/agent-api-contract.md) |
| gRPC server | `AgentRegistrationGrpcService` + lifecycle | [docs/grpc](docs/grpc) |
| REST API | `AgentRegistrationController`, `AgentPayloadController` | [docs/openapi/agent-api.yaml](docs/openapi/agent-api.yaml) |
| Security | registration-token validator, interceptor, filter | [docs/security/registration-security-notes.md](docs/security/registration-security-notes.md) |
| Registry | in-memory + Redis registry, snapshot schema | [docs/registry/registry-schema.md](docs/registry/registry-schema.md) |
| Payloads | submission flow | [docs/payload/payload-submission-flow.md](docs/payload/payload-submission-flow.md) |
| Simulator | `FakeAgentSimulator` | [docs/simulator/fake-agent-simulator.md](docs/simulator/fake-agent-simulator.md) |
| Load test | 10k agents / 20k payloads | [docs/loadtest/10k-load-test-results.md](docs/loadtest/10k-load-test-results.md) |
| Resilience | circuit breaker, bulkhead, fallback, metrics | [docs/resilience](docs/resilience) |
| Dashboard | snapshot + SSE live view | [docs/dashboard/dashboard.md](docs/dashboard/dashboard.md) |
| Errors | unified error body | [docs/error-handling.md](docs/error-handling.md) |

### Run the app

```
.\mvnw.cmd spring-boot:run
# dashboard:   http://localhost:8080/dashboard.html
# REST:        POST http://localhost:8080/api/agents
# gRPC:        localhost:9090  (RegisterAgent/Heartbeat/DeregisterAgent/SubmitPayload)
```

### Key tests

```
.\mvnw.cmd test
.\mvnw.cmd test "-Dtest=TenThousandAgentLoadTest" "-Dagent.load.test=true"
```

Demo script and rehearsal notes: [docs/demo/rehearsal-guide.md](docs/demo/rehearsal-guide.md).
