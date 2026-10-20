package com.zaalima.high_concurrency_agent_orchestrator.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorShouldReturnStructuredBody() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentName\":\"missing-id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/agents"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedBodyShouldReturnStructuredBody() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownAgentHeartbeatShouldReturnStructuredNotFound() throws Exception {
        mockMvc.perform(post("/api/agents/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
