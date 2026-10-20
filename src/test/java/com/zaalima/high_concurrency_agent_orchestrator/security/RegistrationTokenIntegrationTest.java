package com.zaalima.high_concurrency_agent_orchestrator.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = "orchestrator.security.registration-token=test-secret")
@AutoConfigureMockMvc
class RegistrationTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String registerBody() {
        return """
                {"agentId":"%s","agentName":"secure-agent"}
                """.formatted("agent-" + UUID.randomUUID());
    }

    @Test
    void registerWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithWrongTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .header(RegistrationTokenFilter.HEADER_NAME, "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithValidTokenShouldReturn201() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .header(RegistrationTokenFilter.HEADER_NAME, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void listWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isUnauthorized());
    }
}
