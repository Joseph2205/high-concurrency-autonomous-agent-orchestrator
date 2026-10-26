package com.zaalima.high_concurrency_agent_orchestrator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.zaalima.high_concurrency_agent_orchestrator.AbstractRedisIT;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
class AgentRegistrationRestIntegrationTest extends AbstractRedisIT {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueAgentId() {
        return "agent-" + UUID.randomUUID();
    }

    private String registerBody(String agentId) {
        return """
                {
                  "agentId": "%s",
                  "agentName": "test-agent",
                  "endpoint": "http://localhost:9000/callback",
                  "metadata": { "tier": "test" }
                }
                """.formatted(agentId);
    }

    @Test
    void registerShouldReturn201AndAccepted() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueAgentId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.message").value("Agent registered successfully"));
    }

    @Test
    void registerShouldRejectMissingAgentId() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentName\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registeredAgentShouldAppearInList() throws Exception {
        String agentId = uniqueAgentId();

        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(agentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.agentId == '" + agentId + "')]").exists());
    }

    @Test
    void heartbeatForKnownAgentShouldReturnAlive() throws Exception {
        String agentId = uniqueAgentId();

        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(agentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agents/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"" + agentId + "\",\"activeTasks\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true));
    }

    @Test
    void heartbeatForUnknownAgentShouldReturn404() throws Exception {
        mockMvc.perform(post("/api/agents/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"no-such-agent\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deregisterShouldRemoveAgent() throws Exception {
        String agentId = uniqueAgentId();

        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(agentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/agents/" + agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removed").value(true));

        mockMvc.perform(delete("/api/agents/" + agentId))
                .andExpect(status().isNotFound());
    }
}
