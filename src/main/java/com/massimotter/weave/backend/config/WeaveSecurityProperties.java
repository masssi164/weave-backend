package com.massimotter.weave.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weave.security")
public record WeaveSecurityProperties(String requiredAudience) {

    public boolean hasRequiredAudience() {
        return requiredAudience != null && !requiredAudience.isBlank();
    }
}
