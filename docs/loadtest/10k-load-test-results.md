# 10K Agent Load Test Results

Owner: Member 3 (Day 9)
Test: `TenThousandAgentLoadTest`
Command:

```bash
./mvnw test -Dtest=TenThousandAgentLoadTest -Dagent.load.test=true
```

Raw output is written to `target/load-test-report.txt`.

## What it does

- Spawns `10,000` virtual threads (one per simulated agent).
- Each agent registers via `POST /api/agents`, then submits `2` payloads via
  `POST /api/agents/payload` (tasks execute on Member 2's virtual-thread
  executor).
- All threads wait on a single count-down gate so the registration burst is
  truly concurrent, not serialized.

## Measured result (2026-10-20)

| Metric                     | Value |
|----------------------------|-------|
| Agents requested           | 10,000 |
| Agents registered          | 10,000 |
| Payloads submitted         | 20,000 |
| Failures                   | 0 |
| Elapsed                    | 5,669 ms |
| Registration throughput    | ~1,764 agents/sec |

Interpretation: the registration control plane (REST + registry write +
task submission) sustains a 10k concurrent registration burst with zero
failures. This satisfies the mid-review item "Virtual Thread orchestration
engine handling 10k+ concurrent connections" from the registration side;
Member 2 reports the executor-side numbers.

## Notes

- The test is gated behind `-Dagent.load.test=true` so the default CI build
  stays fast (it needs a few GB of heap).
- 20,000 simulated I/O tasks (2s each) are queued to virtual threads; they
  complete asynchronously after the measured window, which is expected.
- Next: rerun with `-Da-registration-load=10000` on the Docker cluster with
  `orchestrator.registry.mode=redis` for the final-review cluster test.
