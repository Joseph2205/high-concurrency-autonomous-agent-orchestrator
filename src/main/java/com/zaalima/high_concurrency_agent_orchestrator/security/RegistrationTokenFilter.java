package com.zaalima.high_concurrency_agent_orchestrator.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RegistrationTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Registration-Token";

    private final RegistrationTokenValidator validator;

    public RegistrationTokenFilter(RegistrationTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !validator.isConfigured()
                || !request.getRequestURI().startsWith("/api/agents");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String presented = request.getHeader(HEADER_NAME);

        if (!validator.matches(presented)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"missing or invalid registration token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
