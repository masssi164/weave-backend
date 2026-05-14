package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.ApiErrorResponse;
import com.massimotter.weave.backend.model.interop.CanonicalBridgeEventResponse;
import com.massimotter.weave.backend.model.interop.InteropStatusResponse;
import com.massimotter.weave.backend.model.interop.SlackEventRequest;
import com.massimotter.weave.backend.model.interop.SlackOAuthCallbackRequest;
import com.massimotter.weave.backend.model.interop.SlackStatusResponse;
import com.massimotter.weave.backend.model.interop.TeamsContractResponse;
import com.massimotter.weave.backend.service.interop.InteropGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Interop", description = "Disabled-by-default external collaboration gateway contracts.")
@SecurityRequirement(name = "bearer-jwt")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Bearer token is missing the weave:workspace scope.",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class InteropController {

    private final InteropGatewayService interopGatewayService;

    public InteropController(InteropGatewayService interopGatewayService) {
        this.interopGatewayService = interopGatewayService;
    }

    @GetMapping("/api/interop/status")
    @Operation(summary = "Get support-safe interop gateway status")
    public InteropStatusResponse status() {
        return interopGatewayService.status();
    }

    @GetMapping("/api/interop/slack/status")
    @Operation(summary = "Get Slack one-channel on-ramp readiness")
    public SlackStatusResponse slackStatus() {
        return interopGatewayService.slackStatus();
    }

    @PostMapping("/api/interop/slack/oauth/callback")
    @Operation(summary = "Accept a Slack OAuth callback skeleton without token exchange")
    @ApiResponse(responseCode = "503", description = "Slack bridge is disabled or secret brokering is not configured.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Map<String, Object> slackOAuthCallback(@Valid @RequestBody SlackOAuthCallbackRequest request) {
        return interopGatewayService.oauthCallback(request);
    }

    @PostMapping("/api/interop/slack/events")
    @Operation(summary = "Convert a Slack text event into a canonical bridge event in sandbox mode")
    @ApiResponse(responseCode = "503", description = "Slack bridge is disabled, unmapped, or not configured.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public CanonicalBridgeEventResponse slackEvent(@Valid @RequestBody SlackEventRequest request) {
        return interopGatewayService.ingestSlackEvent(request);
    }

    @GetMapping("/api/interop/teams/contract")
    @Operation(summary = "Get the Teams gated bridge contract")
    public TeamsContractResponse teamsContract() {
        return interopGatewayService.teamsContract();
    }
}
