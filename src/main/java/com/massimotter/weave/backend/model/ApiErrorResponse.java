package com.massimotter.weave.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable JSON error envelope returned by protected API endpoints.")
public record ApiErrorResponse(
        @Schema(description = "UTC timestamp when the error response was generated.", example = "2026-04-20T11:32:15.123Z")
        String timestamp,
        @Schema(description = "HTTP status code.", example = "401")
        int status,
        @Schema(description = "Short error category.", example = "Unauthorized")
        String error,
        @Schema(description = "Operator-facing explanation of what failed.", example = "Bearer authentication is required to access this endpoint.")
        String message,
        @Schema(description = "Request path that produced the error.", example = "/api/v1/workspace/capabilities")
        String path) {
}
