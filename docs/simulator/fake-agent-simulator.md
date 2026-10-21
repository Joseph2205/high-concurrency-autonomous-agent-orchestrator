# Fake-Agent Simulator

Owner: Member 3 (Day 8)

The simulator registers `N` agents against the running orchestrator, submits a
random number of payloads per agent, then sends a heartbeat. It exists so that
Member 2's virtual-thread executor and Member 1's load balancer can be exercised
with realistic, generated traffic instead of hand-written curl calls.

## Enable

```properties
orchestrator.simulator.enabled=true
orchestrator.simulator.agent-count=1000
orchestrator.simulator.max-submissions-per-agent=3
orchestrator.simulator.base-url=http://localhost:8080
```

Or at startup:

```bash
java -jar orchestrator.jar \
  --orchestrator.simulator.enabled=true \
  --orchestrator.simulator.agent-count=5000
```

## Behaviour

- Agents are simulated on virtual threads (`newVirtualThreadPerTaskExecutor`),
  so the simulator itself scales to thousands of agents.
- Each agent: `POST /api/agents` → 1..M × `POST /api/agents/payload`
  → `POST /api/agents/heartbeat`.
- Failures are logged per agent and do not stop the run.
- Disabled by default; it never runs during tests or normal operation.

## Notes for the mid review

The simulator proves the REST control plane sustains thousands of concurrent
registrations. Member 2 runs the same shape of traffic against the gRPC port
with `grpc-client` for the 10k concurrency milestone.
