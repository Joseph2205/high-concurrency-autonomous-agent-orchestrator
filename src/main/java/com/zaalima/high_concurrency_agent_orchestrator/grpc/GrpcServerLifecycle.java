package com.zaalima.high_concurrency_agent_orchestrator.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger logger =
            LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final AgentRegistrationGrpcService registrationService;
    private final int port;
    private final boolean enabled;

    private volatile Server server;
    private volatile boolean running;

    public GrpcServerLifecycle(
            AgentRegistrationGrpcService registrationService,
            @Value("${orchestrator.grpc.port:9090}") int port,
            @Value("${orchestrator.grpc.enabled:true}") boolean enabled) {

        this.registrationService = registrationService;
        this.port = port;
        this.enabled = enabled;
    }

    @Override
    public void start() {
        if (!enabled) {
            logger.info("gRPC server disabled by configuration");
            return;
        }

        try {
            server = ServerBuilder.forPort(port)
                    .addService(registrationService)
                    .build()
                    .start();

            running = true;
            logger.info("gRPC server started on port {}", port);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (server != null) {
            server.shutdown();
            logger.info("gRPC server stopped");
        }
    }

    @PreDestroy
    public void preDestroy() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    public Server getServer() {
        return server;
    }

    public int getPort() {
        return port;
    }
}
