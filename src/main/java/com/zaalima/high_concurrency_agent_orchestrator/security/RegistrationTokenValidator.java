package com.zaalima.high_concurrency_agent_orchestrator.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegistrationTokenValidator {

    private final String configuredToken;

    public RegistrationTokenValidator(
            @Value("${orchestrator.security.registration-token:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    public boolean isConfigured() {
        return configuredToken != null && !configuredToken.isBlank();
    }

    public boolean matches(String presentedToken) {
        if (!isConfigured()) {
            return true;
        }
        return configuredToken.equals(presentedToken);
    }
}
