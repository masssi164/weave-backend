package com.massimotter.weave.backend.service.connector;

import com.massimotter.weave.backend.config.ConnectorRuntimeProperties;
import com.massimotter.weave.backend.model.connector.ConnectorBoundaryResponse;
import com.massimotter.weave.backend.model.connector.ConnectorManifestValidationRequest;
import com.massimotter.weave.backend.model.connector.ConnectorManifestValidationResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConnectorRuntimeService {

    private final ConnectorRuntimeProperties properties;

    public ConnectorRuntimeService(ConnectorRuntimeProperties properties) {
        this.properties = properties;
    }

    public ConnectorBoundaryResponse boundary() {
        return new ConnectorBoundaryResponse(
                properties.publicSdkEnabled(),
                properties.publicSdkEnabled() ? "experimental-internal" : "deferred",
                List.of(
                        "Connectors run server-side only; Flutter clients never load connector code.",
                        "Credentials are brokered through secret references and never serialized in manifests.",
                        "Quotas, audit, and support-bundle redaction are mandatory for provider connectors."),
                List.of("Slack sandbox hardening", "Teams contract proof", "signed package and pinned dependency policy"));
    }

    public ConnectorManifestValidationResponse validate(ConnectorManifestValidationRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.capabilities() == null || request.capabilities().isEmpty()) {
            errors.add("capabilities must declare at least one scoped capability");
        }
        if (request.secretRefs() != null) {
            request.secretRefs().forEach((name, ref) -> {
                if (ref == null || ref.isBlank()) {
                    errors.add("secretRefs." + name + " must be a non-empty secret reference");
                } else if (looksLikeSecretValue(ref)) {
                    errors.add("secretRefs." + name + " appears to contain secret material instead of a reference");
                }
            });
        }
        return new ConnectorManifestValidationResponse(
                errors.isEmpty(),
                false,
                false,
                List.copyOf(errors),
                List.of("Public connector SDK remains deferred until real Slack and Teams connectors prove the boundary."));
    }

    private boolean looksLikeSecretValue(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("xox") || lower.startsWith("sk-") || lower.contains("bearer ") || value.length() > 80;
    }
}
