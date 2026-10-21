# Registration Security Notes

Owner: Member 3 (Day 4)

## Threat model

Agents register over a shared orchestrator port. Without a shared secret,
any client that can reach the port could register a fake agent and receive
tasks. Day 4 adds a single shared-token check on both transports.

## Mechanism

- Property: `orchestrator.security.registration-token`
- REST: clients send header `X-Registration-Token: <token>` on every
  `/api/agents*` call (`RegistrationTokenFilter`).
- gRPC: clients send ASCII metadata key `registration-token` on every call
  (`RegistrationTokenInterceptor`).
- Shared logic: `RegistrationTokenValidator` — one source of truth for both
  transports.

## Behaviour

| State                        | REST            | gRPC             |
|------------------------------|-----------------|------------------|
| Token not configured (empty) | requests pass   | requests pass    |
| Token configured, header ok  | requests pass   | requests pass    |
| Token configured, header bad | HTTP 401        | `UNAUTHENTICATED`|

Disabled-by-default keeps local development and tests friction-free. For any
shared environment the token must be set.

## Configuration

```properties
orchestrator.security.registration-token=change-me-in-production
```

## Limitations (documented for the mid review)

- Static shared token, not per-agent credentials — rotation requires a
  restart.
- No TLS termination here; production should front the orchestrator with
  TLS (gRPC netty can enable TLS, REST behind a reverse proxy).
- Day 11+ wraps third-party calls with Resilience4j; token auth protects the
  control plane only.
