package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import java.time.Instant;

public record NodeEvent(

        String nodeId,

        NodeEventType eventType,

        Instant timestamp,

        int activeTasks

) {
}