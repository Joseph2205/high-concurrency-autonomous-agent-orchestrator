# Member 3 — Demo & Rehearsal Guide

Owner: Member 3 (Day 20)

Target demo length: **5 minutes**. Rehearse twice end-to-end before the review.

## Pre-flight checklist

1. `.\mvnw.cmd -q test` → green.
2. Redis running (`docker run -p 6379:6379 redis`) if demoing cluster mode.
3. App starts: `.\mvnw.cmd spring-boot:run`.
4. Browser open at `http://localhost:8080/dashboard.html`.
5. Postman collection imported: `docs/postman/agent-orchestrator-collection.json`.
6. `grpcurl` installed for the gRPC part (or use the in-process test).

## Demo script

### 1. REST registration (45s)
- `POST /api/agents` with a new agent → 201, agent appears in "Registered agents".
- Repeat a heartbeat `POST /api/agents/heartbeat` → load counter ticks up.

### 2. Security (30s)
- With `orchestrator.security.registration-token` set, call without the
  `X-Registration-Token` header → **401**. Add header → **200**.

### 3. gRPC registration (45s)
- `grpcurl -plaintext localhost:9090 list`
- `RegisterAgent` with the same token metadata → OK; agent shows on dashboard.
- Show `SubmitPayload` round-trip.

### 4. Resilience (60s)
- Call the flaky external endpoint repeatedly → watch **Circuit breaker** card
  flip CLOSED → OPEN → HALF_OPEN → CLOSED and failure-rate move.
- Point out the **Bulkhead** card limiting concurrent calls and the fallback
  response returned instead of an error.

### 5. Live dashboard (60s)
- Fire the 10k load test in another terminal; narrate:
  `liveVirtualThreads` soaring while `platformThreadCount` stays flat.
- Mention 10k agents / 20k payloads / 0 failures / ~1,764 agents/sec.

### 6. Cluster view (30s)
- Switch `orchestrator.registry.mode=redis`, start a second instance on a
  different port → both nodes appear in the **Cluster nodes** table.

## Likely questions & answers

- **Why virtual threads over a platform pool?** `platformThreadCount` stays near
  core count under 10k load — see `docs/dashboard/virtual-thread-state.md`.
- **How do you know the breaker works?** `FailureSimulationTest` +
  `docs/resilience/failure-test-report.md`.
- **Cross-module contract?** Shared Redis keys `orchestrator:agents` and
  `orchestrator:nodes`; agreed with Member 1 in `docs/registry/registry-schema.md`.
- **What broke and how did you fix it?** Fixed-length virtual-thread monitoring
  via a counting executor (ThreadMXBean cannot see virtual threads).

## Fallback if live demo fails

Show recorded load-test output in `docs/loadtest/10k-load-test-results.md` and
the passing test report from `.\mvnw.cmd test`.
