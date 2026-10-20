# Resilience Metrics (Actuator + Micrometer)

Owner: Member 3 (Day 14)

## Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | application + component health |
| `/actuator/metrics` | list all meters |
| `/actuator/metrics/resilience4j.circuitbreaker.state` | breaker state |
| `/actuator/metrics/resilience4j.circuitbreaker.calls` | call outcomes (timer, tagged `kind`) |
| `/actuator/prometheus` | Prometheus scrape format |

## How binding works

`ResilienceMetricsConfig` binds the Resilience4j **registries** to Micrometer
via `TaggedCircuitBreakerMetrics`, `TaggedBulkheadMetrics`,
`TaggedRetryMetrics` and `TaggedTimeLimiterMetrics`. Registries (not single
instances) are required by the 2.x tagged-metrics API.

## Key meters

| Meter | Tags | Meaning |
|-------|------|---------|
| `resilience4j.circuitbreaker.state` | `name`, `state` | `1.0` for the active state |
| `resilience4j.circuitbreaker.calls` | `name`, `kind` (`successful`/`failed`/`ignored`/`not_permitted`) | call count + latency |
| `resilience4j.circuitbreaker.failure.rate` | `name` | current failure rate |
| `resilience4j.circuitbreaker.not.permitted.calls` | `name`, `kind` | short-circuited calls |
| `resilience4j.bulkhead.available.concurrent.calls` | `name` | free permits |

## Note on fallback semantics

When a fallback is supplied, `ExternalServiceClient` returns the fallback value
as a *successful* result, so the breaker records a `successful` call. That is
intentional: the fallback is our defined healthy outcome. The failed-call
metric increments for unhandled failures (no fallback), which is what the
`ResilienceMetricsIntegrationTest` asserts.

## Evidence

`ResilienceMetricsIntegrationTest`:
- resilience meters are registered on startup,
- a failed guarded call increments `resilience4j.circuitbreaker.calls{kind=failed}`.

## Dashboards

Member 3's terminal dashboard (Days 16–18) reads breaker state from the
`ResilienceRegistry` directly; Prometheus scraping is available for the final
review's observability story.
