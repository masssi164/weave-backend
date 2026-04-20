package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.ApiErrorResponse;
import com.massimotter.weave.backend.model.WorkspaceCapabilitiesResponse;
import com.massimotter.weave.backend.model.WorkspaceReleaseReadinessResponse;
import com.massimotter.weave.backend.service.WorkspaceCapabilityService;
import com.massimotter.weave.backend.service.WorkspaceReleaseReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace")
@Tag(name = "Workspace", description = "Workspace readiness and capability endpoints.")
public class WorkspaceController {

    private final WorkspaceCapabilityService workspaceCapabilityService;
    private final WorkspaceReleaseReadinessService workspaceReleaseReadinessService;

    public WorkspaceController(
            WorkspaceCapabilityService workspaceCapabilityService,
            WorkspaceReleaseReadinessService workspaceReleaseReadinessService) {
        this.workspaceCapabilityService = workspaceCapabilityService;
        this.workspaceReleaseReadinessService = workspaceReleaseReadinessService;
    }

    @GetMapping("/capabilities")
    @Operation(
            summary = "Get workspace capability readiness",
            description = "Returns the backend-owned workspace capability snapshot consumed by the Weave client.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Workspace capability snapshot.",
                    content = @Content(schema = @Schema(implementation = WorkspaceCapabilitiesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Bearer token is missing the weave:workspace scope.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public WorkspaceCapabilitiesResponse capabilities() {
        return workspaceCapabilityService.snapshot();
    }

    @GetMapping("/release-readiness")
    @Operation(
            summary = "Get Release 1 workspace readiness",
            description = "Returns an operator-facing snapshot of the backend-owned Release 1 dependencies and remaining setup actions.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Workspace Release 1 readiness snapshot.",
                    content = @Content(schema = @Schema(implementation = WorkspaceReleaseReadinessResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Bearer token is missing the weave:workspace scope.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public WorkspaceReleaseReadinessResponse releaseReadiness() {
        return workspaceReleaseReadinessService.snapshot();
    }
}
