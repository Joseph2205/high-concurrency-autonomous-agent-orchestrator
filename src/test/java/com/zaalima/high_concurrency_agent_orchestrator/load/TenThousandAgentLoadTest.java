package com.zaalima.high_concurrency_agent_orchestrator.load;

import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentPayloadRequest;
import com.zaalima.high_concurrency_agent_orchestrator.dto.AgentRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;

/**
 * 10k-agent concurrency test for the registration control plane.
 *
 * <p>Excluded from the normal build because it needs a large heap and takes a
 * few minutes. Run explicitly:
 *
 * <pre>
 *   ./mvnw test -Dtest=TenThousandAgentLoadTest -Dagent.load.test=true \
 *       -Da-registration-load=10000
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "agent.load.test", matches = "true")
class TenThousandAgentLoadTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void tenThousandAgentsShouldRegisterAndSubmitUnderLoad() throws Exception {
        int agentCount = Integer.getInteger("a-registration-load", 10000);
        int submissionsPerAgent = 2;

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger registered = new AtomicInteger();
        AtomicInteger submitted = new AtomicInteger();
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());

        long startNanos = System.nanoTime();

        for (int i = 0; i < agentCount; i++) {
            String agentId = "load-agent-" + i;
            executor.submit(() -> {
                try {
                    startGate.await();

                    performPost("/api/agents",
                            new AgentRegistrationRequest(
                                    agentId, "load-agent", null, Map.of()));
                    registered.incrementAndGet();

                    for (int s = 0; s < submissionsPerAgent; s++) {
                        performPost("/api/agents/payload",
                                new AgentPayloadRequest(
                                        agentId, "load-task-" + s, Map.of("i", String.valueOf(s))));
                        submitted.incrementAndGet();
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.MINUTES),
                "load test did not finish in time");

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        String report = String.format(
                "Load test: agents=%d registered=%d payloads=%d failures=%d elapsedMs=%d throughputPerSec=%.1f%n",
                agentCount, registered.get(), submitted.get(), failures.size(), elapsedMs,
                agentCount * 1000.0 / Math.max(1, elapsedMs));

        java.nio.file.Files.writeString(
                java.nio.file.Path.of("target", "load-test-report.txt"), report);
        System.out.print(report);

        if (!failures.isEmpty()) {
            System.out.println("First failure: " + failures.get(0));
        }

        assertEquals(agentCount, registered.get(), "all agents should register");
        assertEquals(agentCount * submissionsPerAgent, submitted.get(),
                "all payloads should be accepted");
        assertEquals(0, failures.size(), "no request should fail under load");
    }

    private void performPost(String path, Object body) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful());
    }
}
