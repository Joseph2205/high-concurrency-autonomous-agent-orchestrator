package com.zaalima.high_concurrency_agent_orchestrator.grpc;

import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.agent.InMemoryAgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.AgentRegistrationServiceGrpc;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.DeregisterAgentRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.DeregisterAgentResponse;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.HeartbeatRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.HeartbeatResponse;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.RegisterAgentRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.RegisterAgentResponse;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRegistrationGrpcIntegrationTest {

    private Server server;
    private ManagedChannel channel;
    private AgentRegistrationServiceGrpc.AgentRegistrationServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "in-process-" + UUID.randomUUID();

        AgentRegistry registry = new InMemoryAgentRegistry("test-node");
        AgentRegistrationGrpcService service = new AgentRegistrationGrpcService(
                registry,
                new com.zaalima.high_concurrency_agent_orchestrator.service
                        .AgentPayloadService(
                                registry,
                                new com.zaalima.high_concurrency_agent_orchestrator
                                        .AgentTaskService(
                                                java.util.concurrent.Executors
                                                        .newVirtualThreadPerTaskExecutor())));

        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(service)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(name)
                .directExecutor()
                .build();

        stub = AgentRegistrationServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        channel.awaitTermination(5, TimeUnit.SECONDS);
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void registerShouldBeAccepted() {
        RegisterAgentResponse response = stub.registerAgent(
                RegisterAgentRequest.newBuilder()
                        .setAgentId("agent-grpc-1")
                        .setAgentName("grpc-agent")
                        .setEndpoint("http://localhost:9000")
                        .putMetadata("region", "test")
                        .build());

        assertTrue(response.getAccepted());
        assertEquals("test-node", response.getNodeId());
    }

    @Test
    void registerWithEmptyAgentIdShouldFail() {
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> stub.registerAgent(
                        RegisterAgentRequest.newBuilder()
                                .setAgentId("")
                                .setAgentName("grpc-agent")
                                .build()));

        assertEquals(Status.INVALID_ARGUMENT.getCode(), ex.getStatus().getCode());
    }

    @Test
    void heartbeatForRegisteredAgentShouldBeAlive() {
        stub.registerAgent(RegisterAgentRequest.newBuilder()
                .setAgentId("agent-grpc-2")
                .setAgentName("grpc-agent")
                .build());

        HeartbeatResponse response = stub.heartbeat(
                HeartbeatRequest.newBuilder()
                        .setAgentId("agent-grpc-2")
                        .setActiveTasks(5)
                        .build());

        assertTrue(response.getAlive());
        assertTrue(response.getServerTimeEpochMs() > 0);
    }

    @Test
    void heartbeatForUnknownAgentShouldNotBeAlive() {
        HeartbeatResponse response = stub.heartbeat(
                HeartbeatRequest.newBuilder()
                        .setAgentId("ghost")
                        .build());

        assertFalse(response.getAlive());
    }

    @Test
    void deregisterShouldRemoveAgent() {
        stub.registerAgent(RegisterAgentRequest.newBuilder()
                .setAgentId("agent-grpc-3")
                .setAgentName("grpc-agent")
                .build());

        DeregisterAgentResponse response = stub.deregisterAgent(
                DeregisterAgentRequest.newBuilder()
                        .setAgentId("agent-grpc-3")
                        .build());

        assertTrue(response.getRemoved());

        HeartbeatResponse afterRemoval = stub.heartbeat(
                HeartbeatRequest.newBuilder()
                        .setAgentId("agent-grpc-3")
                        .build());

        assertFalse(afterRemoval.getAlive());
    }
}
