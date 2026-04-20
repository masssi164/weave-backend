package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.config.WeaveSecurityProperties;
import com.massimotter.weave.backend.config.WorkspaceCapabilityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceCapabilityServiceTest {

    @Test
    void marksChatAndFilesDegradedUntilTheirRoutesAreConfigured() {
        WorkspaceCapabilityService service = new WorkspaceCapabilityService(
                resourceServerProperties("https://auth.example.invalid/realms/weave"),
                new WeaveSecurityProperties("weave-app", "weave-app"),
                new WorkspaceCapabilityProperties(null, null, null, null, null));

        var snapshot = service.snapshot();

        assertThat(snapshot.shellAccess().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.READY);
        assertThat(snapshot.chat().enabled()).isTrue();
        assertThat(snapshot.chat().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.DEGRADED);
        assertThat(snapshot.files().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.DEGRADED);
        assertThat(snapshot.calendar().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.UNAVAILABLE);
    }

    @Test
    void blocksDependentCapabilitiesWhenShellAccessCannotValidateTokens() {
        WorkspaceCapabilityService service = new WorkspaceCapabilityService(
                resourceServerProperties(null),
                new WeaveSecurityProperties("weave-app", "weave-app"),
                new WorkspaceCapabilityProperties(
                        new WorkspaceCapabilityProperties.Capability(true, null),
                        new WorkspaceCapabilityProperties.Capability(true, "https://matrix.weave.local"),
                        new WorkspaceCapabilityProperties.Capability(true, "https://nextcloud.weave.local"),
                        null,
                        null));

        var snapshot = service.snapshot();

        assertThat(snapshot.shellAccess().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.BLOCKED);
        assertThat(snapshot.chat().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.BLOCKED);
        assertThat(snapshot.files().readiness()).isEqualTo(com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness.BLOCKED);
    }

    private OAuth2ResourceServerProperties resourceServerProperties(String issuerUri) {
        OAuth2ResourceServerProperties properties = new OAuth2ResourceServerProperties();
        properties.getJwt().setIssuerUri(issuerUri);
        return properties;
    }
}
