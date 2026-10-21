package com.zaalima.high_concurrency_agent_orchestrator.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, path);
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "timestamp", timestamp.toString(),
                "status", status,
                "error", error,
                "message", message == null ? "" : message,
                "path", path == null ? "" : path);
    }
}
