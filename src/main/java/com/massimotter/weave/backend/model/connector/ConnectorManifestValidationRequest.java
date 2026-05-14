package com.massimotter.weave.backend.model.connector;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ConnectorManifestValidationRequest(
        @NotBlank @Size(max = 128) String id,
        @NotBlank @Size(max = 128) String provider,
        List<@Size(max = 128) String> capabilities,
        Map<String, String> secretRefs) {
}
