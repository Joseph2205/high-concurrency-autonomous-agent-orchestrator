# Cluster-Wide Dashboard View

Owner: Member 3 (Day 18)

## Goal

One dashboard that shows every orchestrator node, not just the local one.

## Shared state

| Key | Type | Field | Value |
|-----|------|-------|-------|
| `orchestrator:nodes` | Hash | `nodeId` | `<lastHeartbeatEpochMs>|<activeTasks>` |

## Writers

`ClusterNodeHeartbeat` (Redis mode only) publishes this node's heartbeat every
`orchestrator.registry.node-heartbeat-ms` (default 5s). It runs on Spring's
scheduler (`@EnableScheduling`).

Example:

```
orchestrator:nodes
  node-1 -> 1760900123456|12
  node-2 -> 1760900123400|8
  node-3 -> 1760900119000|0
```

## Reader

`ClusterNodeProvider` (Redis mode only) reads the hash for the dashboard. A node
is `alive` when `now - lastHeartbeat <= orchestrator.registry.stale-after-seconds`.
The dashboard's **Cluster nodes** table renders one row per node.

## Modes

| Mode | Behaviour |
|------|-----------|
| `memory` (default) | cluster table shows "single-node mode" |
| `redis` | cluster table lists all live nodes with active-task load |

`DashboardService` injects `ClusterNodeProvider` through an `ObjectProvider`,
so memory mode (dev/tests) never fails on a missing bean.

## Boundary with Member 1

Member 1 owns node discovery. This module only **reads** `orchestrator:nodes`
and writes its own node's heartbeat entry; it does not perform leader election
or locking. The key names are agreed so both modules share state cleanly.
