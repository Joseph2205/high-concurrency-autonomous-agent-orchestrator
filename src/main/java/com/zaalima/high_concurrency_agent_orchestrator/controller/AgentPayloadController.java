package com.zaalima.high_concurrency_agent_orchestrator.controller;

import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadResponse;
import com.zaalima.high_concurrency_agent_orchestrator.service.AgentPayloadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentPayloadController {

    private final AgentPayloadService payloadService;

    public AgentPayloadController(AgentPayloadService payloadService) {
        this.payloadService = payloadService;
    }

    @PostMapping("/payload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AgentPayloadResponse submitPayload(
            @Valid @RequestBody AgentPayloadRequest request) {

        return payloadService.submit(
                request.agentId(),
                request.taskName(),
                request.payload());
    }
}
