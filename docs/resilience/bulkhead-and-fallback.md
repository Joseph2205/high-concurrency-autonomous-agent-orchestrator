# Bulkhead and Fallback Behaviour

Owner: Member 3 (Day 12)

## Bulkhead

A semaphore bulkhead caps how many outbound third-party calls run at once, so
one slow dependency cannot occupy every virtual thread and starve the rest of
the orchestrator.

| Property                     | Default | Meaning |
|------------------------------|---------|---------|
| max-bulkhead-concurrent-calls| 50      | concurrent outbound calls |
| max-bulkhead-wait            | 500ms   | how long a call waits for a slot before being rejected |

Behaviour when saturated:
- A call waits up to `max-bulkhead-wait` for a free permit.
- If no permit frees up, it fails fast with `BulkheadFullException`.
- The failure is a normal `RuntimeException`, so the circuit breaker counts it
  and the supplied fallback runs.

## Fallback

`ExternalServiceClient.call(externalCall, fallback)` runs the fallback whenever
the guarded call throws — circuit open, bulkhead full, retry exhausted, or a
timeout. Fallbacks must be:

- fast and side-effect free,
- able to return a degraded but valid value,
- never throwing themselves.

## Composition order

```
TimeLimiter  ->  Retry  ->  Bulkhead  ->  CircuitBreaker  ->  upstream
```

Rationale:
- **TimeLimiter outermost** so a stuck upstream is abandoned quickly and its
  virtual thread is released.
- **Retry outside bulkhead** so retries re-acquire a permit rather than holding
  one during back-off.
- **CircuitBreaker innermost** so it only observes genuine upstream outcomes,
  not our own bulkhead/timeout rejects.

## Evidence

`ExternalServiceClientBulkheadTest`:
- 20 concurrent callers against a 2-permit bulkhead → some callers get the
  fallback,
- bulkhead metrics report the configured limit,
- a `BulkheadFullException` maps to the fallback value.
