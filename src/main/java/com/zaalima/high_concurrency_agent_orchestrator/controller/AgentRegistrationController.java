package com.zaalima.high_concurrency_agent_orchestrator.controller;

import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentInfo;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistryService;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentDeregisterResponse;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentHeartbeatRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentHeartbeatResponse;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentRegistrationRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentRegistrationResponse;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentRegistrationController {

    private final AgentRegistryService registry;

    public AgentRegistrationController(AgentRegistryService registry) {
        this.registry = registry;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentRegistrationResponse register(
            @Valid @RequestBody AgentRegistrationRequest request) {

        AgentInfo info = registry.register(
                request.agentId(),
                request.agentName(),
                request.endpoint(),
                request.metadata());

        return new AgentRegistrationResponse(
                true,
                "Agent registered successfully",
                registry.getNodeId());
    }

    @PostMapping("/heartbeat")
    public AgentHeartbeatResponse heartbeat(
            @Valid @RequestBody AgentHeartbeatRequest request) {

        boolean alive = registry.heartbeat(
                request.agentId(),
                request.activeTasks());

        if (!alive) {
            throw new AgentNotRegisteredException(request.agentId());
        }

        return new AgentHeartbeatResponse(true, System.currentTimeMillis());
    }

    @DeleteMapping("/{agentId}")
    public AgentDeregisterResponse deregister(@PathVariable String agentId) {

        boolean removed = registry.deregister(agentId);
        if (!removed) {
            throw new AgentNotRegisteredException(agentId);
        }

        return new AgentDeregisterResponse(true);
    }

    @GetMapping
    public List<AgentSummaryResponse> listAgents() {
        return registry.findAll().stream()
                .map(info -> new AgentSummaryResponse(
                        info.getAgentId(),
                        info.getAgentName(),
                        info.getEndpoint(),
                        info.getMetadata(),
                        info.getRegisteredAtEpochMs(),
                        info.getLastHeartbeatEpochMs(),
                        info.getActiveTasks(),
                        registry.getNodeId()))
                .toList();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class AgentNotRegisteredException extends RuntimeException {
        public AgentNotRegisteredException(String agentId) {
            super("Agent not registered: " + agentId);
        }
    }
}
