package com.zaalima.high_concurrency_agent_orchestrator.grpc;

import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentInfo;
import com.zaalima.high_concurrency_agent_orchestrator.agent.AgentRegistry;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.AgentRegistrationServiceGrpc;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.DeregisterAgentRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.DeregisterAgentResponse;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.HeartbeatRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.HeartbeatResponse;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.RegisterAgentRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.RegisterAgentResponse;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.SubmitPayloadRequest;
import com.zaalima.high_concurrency_agent_orchestrator.protocol.SubmitPayloadResponse;
import com.zaalima.high_concurrency_agent_orchestrator.service.AgentPayloadService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistrationGrpcService
        extends AgentRegistrationServiceGrpc.AgentRegistrationServiceImplBase {

    private static final Logger logger =
            LoggerFactory.getLogger(AgentRegistrationGrpcService.class);

    private final AgentRegistry registry;
    private final AgentPayloadService payloadService;

    public AgentRegistrationGrpcService(
            AgentRegistry registry,
            AgentPayloadService payloadService) {
        this.registry = registry;
        this.payloadService = payloadService;
    }

    @Override
    public void registerAgent(
            RegisterAgentRequest request,
            StreamObserver<RegisterAgentResponse> responseObserver) {

        if (request.getAgentId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("agent_id must not be empty")
                    .asRuntimeException());
            return;
        }

        if (request.getAgentName().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("agent_name must not be empty")
                    .asRuntimeException());
            return;
        }

        AgentInfo info = registry.register(
                request.getAgentId(),
                request.getAgentName(),
                request.getEndpoint(),
                request.getMetadataMap());

        logger.info(
                "gRPC: agent {} registered on node {}",
                info.getAgentId(),
                registry.getNodeId());

        RegisterAgentResponse response = RegisterAgentResponse.newBuilder()
                .setAccepted(true)
                .setMessage("Agent registered successfully")
                .setNodeId(registry.getNodeId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(
            HeartbeatRequest request,
            StreamObserver<HeartbeatResponse> responseObserver) {

        boolean alive = registry.heartbeat(
                request.getAgentId(),
                request.getActiveTasks());

        HeartbeatResponse response = HeartbeatResponse.newBuilder()
                .setAlive(alive)
                .setServerTimeEpochMs(System.currentTimeMillis())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deregisterAgent(
            DeregisterAgentRequest request,
            StreamObserver<DeregisterAgentResponse> responseObserver) {

        boolean removed = registry.deregister(request.getAgentId());

        logger.info("gRPC: agent {} deregistered (removed={})",
                request.getAgentId(), removed);

        DeregisterAgentResponse response = DeregisterAgentResponse.newBuilder()
                .setRemoved(removed)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void submitPayload(
            SubmitPayloadRequest request,
            StreamObserver<SubmitPayloadResponse> responseObserver) {

        try {
            var result = payloadService.submit(
                    request.getAgentId(),
                    request.getTaskName(),
                    request.getPayloadMap());

            SubmitPayloadResponse response = SubmitPayloadResponse.newBuilder()
                    .setAccepted(result.accepted())
                    .setTaskId(result.taskId())
                    .setStatus(result.status())
                    .setMessage(result.message())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (com.zaalima.high_concurrency_agent_orchestrator.controller
                .AgentRegistrationController.AgentNotRegisteredException ex) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(ex.getMessage())
                    .asRuntimeException());
        }
    }
}
