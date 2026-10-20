# Agent Registry Schema (Redis)

Owner: Member 3 (Day 6)
Coordinates with: Member 1 (Redis Pub/Sub and distributed locking)

## Key

| Property | Value |
|----------|-------|
| Redis key | `orchestrator:agents` |
| Redis type | Hash |
| Hash field | `agentId` (string) |
| Hash value | JSON `AgentSnapshot` (string) |

## JSON record (AgentSnapshot)

```json
{
  "agentId": "agent-001",
  "agentName": "analytics-agent",
  "endpoint": "http://10.0.0.5:9000/callback",
  "metadata": { "region": "eu-west-1" },
  "registeredAtEpochMs": 1760900000000,
  "lastHeartbeatEpochMs": 1760900060000,
  "activeTasks": 4
}
```

## Consistency model

- Register → `HSET orchestrator:agents <agentId> <json>`
- Heartbeat → `HGET` + update + `HSET`
- Deregister → `HDEL`
- List → `HVALS`

Every orchestrator node configured with `orchestrator.registry.mode=redis`
reads and writes the same hash, so an agent registered on node-1 is visible
to node-2 on the next `GET /api/agents`.

## Liveness

The registry does not delete stale agents by itself. A heartbeat older than
`orchestrator.registry.stale-after-seconds` should be treated as gone by
consumers (dashboard greys the row). Per-field TTL is not available in
Redis hashes; if hard expiry is required, move each agent to its own
`orchestrator:agent:<id>` string key with `SET ... PX` (follow-up, not in
the 20-day scope).

## Modes

| `orchestrator.registry.mode` | Implementation            | Use case |
|------------------------------|---------------------------|----------|
| `memory` (default)           | `InMemoryAgentRegistry`   | local dev, unit tests |
| `redis`                      | `RedisAgentRegistry`      | multi-node Docker cluster |

## Overlap with Member 1 keys

Member 1 owns `orchestrator:lock:*` (distributed locks) and Pub/Sub
channels. This hash is the only key owned by Member 3 — do not write it
from other modules.
