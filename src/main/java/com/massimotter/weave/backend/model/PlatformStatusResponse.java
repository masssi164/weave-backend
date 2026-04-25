package com.massimotter.weave.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Backend-owned status snapshot for admin diagnostics and smoke tests.")
public record PlatformStatusResponse(
        SimpleStatus backend,
        SimpleStatus auth,
        MatrixStatus matrix,
        SimpleStatus nextcloud) {

    public record SimpleStatus(String status) {
    }

    public record MatrixStatus(String status, boolean federationEnabled, boolean e2eeEnabled) {
    }
}
