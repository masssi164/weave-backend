package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.WorkspaceCapabilitiesResponse;
import com.massimotter.weave.backend.model.WorkspaceCapabilityReadiness;
import com.massimotter.weave.backend.model.WorkspaceCapabilityStatusResponse;
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
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.")
    })
    public WorkspaceCapabilitiesResponse capabilities() {
        return new WorkspaceCapabilitiesResponse(
                new WorkspaceCapabilityStatusResponse(true, WorkspaceCapabilityReadiness.READY),
                new WorkspaceCapabilityStatusResponse(true, WorkspaceCapabilityReadiness.READY),
                new WorkspaceCapabilityStatusResponse(true, WorkspaceCapabilityReadiness.READY),
                new WorkspaceCapabilityStatusResponse(false, WorkspaceCapabilityReadiness.UNAVAILABLE),
                new WorkspaceCapabilityStatusResponse(false, WorkspaceCapabilityReadiness.UNAVAILABLE));
    }
}
