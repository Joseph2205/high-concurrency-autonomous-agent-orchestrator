# Circuit Breaker Design

Owner: Member 3 (Day 11)

## Purpose

Third-party agent APIs are outside our control; when they degrade, an
orchestrator that keeps calling them burns virtual threads and carrier
threads on hopeless requests. A circuit breaker converts those failures into
fast, local rejections so the orchestrator stays responsive.

## States (Resilience4j semantics)

```
            failure rate >= threshold
   CLOSED ───────────────────────────► OPEN
     ▲                                   │
     │ trials succeed          waitDurationInOpenState elapses
     │                                   ▼
     └──────────── HALF_OPEN ◄───────────┘
                 (permittedNumberOfCallsInHalfOpenState probes)
```

- **CLOSED** — calls flow normally; failure rate is measured over a sliding
  window.
- **OPEN** — calls fail immediately (`CallNotPermittedException`), no upstream
  traffic; this is where the fallback fires.
- **HALF_OPEN** — a limited number of probe calls decide whether to close or
  reopen.

## Configuration (`orchestrator.resilience.*`)

| Property                            | Default | Meaning |
|-------------------------------------|---------|---------|
| failure-rate-threshold              | 50%     | open when failing |
| sliding-window-size                 | 10      | calls measured |
| minimum-number-of-calls             | 5       | before rate is judged |
| permitted-calls-in-half-open-state  | 3       | probe calls |
| wait-duration-in-open-state         | 10s     | cool-down before probing |

## Where it lives

- `ResilienceRegistry` — builds the four Resilience4j primitives (circuit
  breaker, bulkhead, retry, time limiter) once and exposes them.
- `ExternalServiceClient` — composes them around any `Supplier` and applies a
  fallback. Call order (outermost first):
  **TimeLimiter → Retry → Bulkhead → CircuitBreaker → upstream**.
- State transitions are logged by the event publisher, which is what Member 3's
  dashboard later renders.

## Why programmatic, not `@CircuitBreaker`

The project runs on Spring Boot 4.x, whose dependency management does not ship
the `spring-boot-starter-aop` the annotation-based Resilience4j starter expects.
The programmatic API is equally valid (same Resilience4j core) and keeps the
retry/timeout/bulkhead composition explicit, which is easier to demonstrate in
the review.

## Evidence

`ExternalServiceClientCircuitBreakerTest`:
- opens after consecutive failures,
- stops forwarding once open,
- returns the fallback on failure,
- returns the upstream value on success.
