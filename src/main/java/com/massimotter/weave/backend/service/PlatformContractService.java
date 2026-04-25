package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.config.PlatformContractProperties;
import com.massimotter.weave.backend.config.WorkspaceCapabilityProperties;
import com.massimotter.weave.backend.model.PlatformConfigResponse;
import com.massimotter.weave.backend.model.PlatformStatusResponse;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.stereotype.Service;

@Service
public class PlatformContractService {

    private final OAuth2ResourceServerProperties resourceServerProperties;
    private final PlatformContractProperties platformProperties;
    private final WorkspaceCapabilityProperties workspaceProperties;

    public PlatformContractService(
            OAuth2ResourceServerProperties resourceServerProperties,
            PlatformContractProperties platformProperties,
            WorkspaceCapabilityProperties workspaceProperties) {
        this.resourceServerProperties = resourceServerProperties;
        this.platformProperties = platformProperties;
        this.workspaceProperties = workspaceProperties;
    }

    public PlatformConfigResponse config() {
        return new PlatformConfigResponse(
                platformProperties.publicBaseUrl(),
                platformProperties.apiBaseUrl(),
                platformProperties.authBaseUrl(),
                platformProperties.matrixBaseUrl(),
                platformProperties.matrixBaseUrl(),
                platformProperties.filesProductUrl(),
                platformProperties.calendarProductUrl(),
                platformProperties.nextcloudRawBaseUrl(),
                platformProperties.nextcloudRawBaseUrl(),
                new PlatformConfigResponse.Targets(
                        platformProperties.targets().mobile(),
                        platformProperties.targets().desktop(),
                        platformProperties.targets().web()),
                new PlatformConfigResponse.Features(
                        workspaceProperties.chat().enabled(),
                        false,
                        false,
                        workspaceProperties.files().enabled(),
                        workspaceProperties.calendar().enabled()));
    }

    public PlatformStatusResponse status() {
        return new PlatformStatusResponse(
                new PlatformStatusResponse.SimpleStatus("up"),
                new PlatformStatusResponse.SimpleStatus(hasText(resourceServerProperties.getJwt().getIssuerUri())
                        ? "up"
                        : "blocked"),
                new PlatformStatusResponse.MatrixStatus(moduleStatus(workspaceProperties.chat()), false, false),
                new PlatformStatusResponse.SimpleStatus(moduleStatus(workspaceProperties.files())));
    }

    private String moduleStatus(WorkspaceCapabilityProperties.Capability capability) {
        if (!capability.enabled()) {
            return "disabled";
        }
        if (capability.readiness() != null) {
            return capability.readiness().name().toLowerCase();
        }
        return hasText(capability.dependencyUrl()) ? "up" : "degraded";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
