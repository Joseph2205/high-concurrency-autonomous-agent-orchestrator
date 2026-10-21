# Terminal / Web Dashboard

Owner: Member 3 (Days 16–18)

The dashboard shows live orchestrator state. It is implemented as a
Spring Boot web view plus a JSON/SSE API, which is simpler to demo in a
browser than a JLine terminal and reuses the same controllers.

## Open it

Start the app, then visit:

```
http://localhost:8080/dashboard.html
```

## API

| Method | Path                       | Purpose |
|--------|----------------------------|---------|
| GET    | `/api/dashboard/snapshot`  | one JSON snapshot |
| GET    | `/api/dashboard/stream`    | SSE stream, one snapshot per second |

## Snapshot fields

| Field | Meaning |
|-------|---------|
| nodeId | this orchestrator node |
| registeredAgents | agents in the registry |
| activeTasksReportedByAgents | sum of agent-reported load |
| taskCounts | running / completed / failed |
| recentTasks | last 25 tasks with status + virtual-thread flag |
| clusterNodes | other nodes (populated in Day 18) |
| circuitBreakerState | CLOSED / OPEN / HALF_OPEN |
| circuitBreakerFailureRate | current failure rate |
| bulkheadAvailablePermits / bulkheadMaxPermits | bulkhead capacity |

## Source

- `DashboardController` — REST + SSE endpoints.
- `DashboardService` — assembles the snapshot from the registry, Resilience4j
  primitives and the task provider.
- `DashboardTaskProvider` — reads Member 2's `AgentTaskService`.
- `src/main/resources/static/dashboard.html` — the view (polls the snapshot
  once per second; SSE endpoint available for lower-latency clients).

## Day 17 (virtual-thread states)

Each task row carries a `virtualThread` flag taken from the executor that ran
it (Member 2 runs every task on `Executors.newVirtualThreadPerTaskExecutor()`),
so the dashboard distinguishes virtual-thread execution from any future
platform-thread path.

## Day 18 (cluster view)

When `orchestrator.registry.mode=redis`, the `clusterNodes` field is populated
from the shared node registry so a single dashboard shows every orchestrator
node. See `docs/dashboard/cluster-view.md`.
