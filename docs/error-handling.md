# Error Handling

Owner: Member 3 (Day 19)

## Unified error body

Every error response now uses the same JSON shape (`ApiError`):

```json
{
  "timestamp": "2026-10-20T11:52:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Agent not registered: ghost",
  "path": "/api/agents/heartbeat"
}
```

## Handled cases

| Exception | Status | Notes |
|-----------|--------|-------|
| `MethodArgumentNotValidException` | 400 | field-level validation message |
| `HttpMessageNotReadableException` | 400 | malformed / missing JSON body |
| `IllegalArgumentException` | 400 | bad task input |
| `AgentNotRegisteredException` | 404 | heartbeat / payload for unknown agent |
| `Exception` (catch-all) | 500 | logged server-side, generic client message |

## Security note

The catch-all handler logs the full stack trace server-side but returns only
"Unexpected server error" to the client, so internal details are never leaked.

## Token filter

`RegistrationTokenFilter` returns `401` with a `{"error": ...}` body when the
token is missing/invalid, matching the same error style at the filter layer
(filters run before `@RestControllerAdvice`).

## Evidence

`ErrorHandlingIntegrationTest`:
- validation error → 400 structured body,
- malformed JSON → 400,
- unknown-agent heartbeat → 404 with `error: Not Found`.

## Dashboard resilience

The dashboard poller treats any failed snapshot fetch as a red connection dot
and keeps polling, so a transient backend error never blanks the view.
