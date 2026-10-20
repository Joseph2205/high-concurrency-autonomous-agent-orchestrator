# Failure Test Report (Resilience)

Owner: Member 3 (Day 13)
Test suite: `FailureSimulationTest`, `ExternalServiceClientCircuitBreakerTest`,
`ExternalServiceClientBulkheadTest`

## Failure modes injected

| Mode        | Simulation                                   | Expected orchestrator behaviour |
|-------------|----------------------------------------------|---------------------------------|
| Slow        | Upstream sleeps far longer than the timeout  | TimeLimiter aborts; fallback returns |
| HTTP 500    | Upstream throws `UpstreamServerException`    | Retry, then fallback; breaker counts it |
| Disconnect  | Upstream throws `UpstreamConnectionException`| Treated like any upstream fault |
| Recovery    | Upstream returns to OK after cool-down       | Breaker HALF_OPEN → CLOSED |

## Results

| Scenario | Assertion | Result |
|----------|-----------|--------|
| Slow upstream | fallback value returned, no hang | Pass |
| HTTP 500 x6 | 6/6 handled by fallback, breaker OPEN, no exception leaks | Pass |
| Open circuit | call short-circuits, upstream invocation count unchanged | Pass |
| Recovery | after `waitDurationInOpenState`, breaker returns to CLOSED and probes reach the healed upstream | Pass |
| Circuit opens | opens after consecutive failures, stops forwarding | Pass |
| Bulkhead saturation | excess callers get fallback, limit respected | Pass |

## Cascading-failure conclusion

No failure from the simulated upstream propagated past `ExternalServiceClient`.
Every failure was converted into a fallback value; once the breaker opened, the
unreachable upstream received zero further calls. This is the evidence required
for the final-review item "Resilience patterns preventing cascading network
failures".

## How to reproduce

```bash
./mvnw test -Dtest='*Resilience*,FailureSimulationTest,ExternalServiceClient*Test'
```
