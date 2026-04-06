package com.massimotter.weave.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weave.security")
public record WeaveSecurityProperties(String requiredAudience, List<String> allowedAzp) {

    public boolean hasRequiredAudience() {
        return requiredAudience != null && !requiredAudience.isBlank();
    }

    /**
     * Returns true when the azp allowlist is non-empty (enforcement is active).
     * An allowlist containing only blank strings is treated as disabled.
     */
    public boolean hasAllowedAzp() {
        return allowedAzp != null
                && !allowedAzp.isEmpty()
                && allowedAzp.stream().anyMatch(s -> !s.isBlank());
    }

    /**
     * Returns the effective allowlist with blank entries removed.
     * Returns an empty list when enforcement is disabled.
     */
    public List<String> effectiveAllowedAzp() {
        if (allowedAzp == null) {
            return List.of();
        }
        return allowedAzp.stream()
                .filter(s -> s != null && !s.isBlank())
                .toList();
    }
}
