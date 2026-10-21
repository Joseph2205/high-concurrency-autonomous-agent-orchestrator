package com.zaalima.high_concurrency_agent_orchestrator.grpc;

import com.zaalima.high_concurrency_agent_orchestrator.security.RegistrationTokenValidator;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class RegistrationTokenInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("registration-token", Metadata.ASCII_STRING_MARSHALLER);

    private final RegistrationTokenValidator validator;

    public RegistrationTokenInterceptor(RegistrationTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        if (!validator.isConfigured()) {
            return next.startCall(call, headers);
        }

        String presented = headers.get(TOKEN_KEY);
        if (!validator.matches(presented)) {
            call.close(Status.UNAUTHENTICATED
                    .withDescription("missing or invalid registration token"), headers);
            return new ServerCall.Listener<>() {
            };
        }

        return Contexts.interceptCall(Context.current(), call, headers, next);
    }
}
