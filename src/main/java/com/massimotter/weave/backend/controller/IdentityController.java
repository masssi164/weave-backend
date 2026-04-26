package com.massimotter.weave.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.massimotter.weave.backend.model.AuthenticatedUserResponse;
import com.massimotter.weave.backend.model.ApiErrorResponse;
import com.massimotter.weave.backend.model.ModuleSyncStatusResponse;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Identity", description = "Authenticated caller introspection endpoints.")
public class IdentityController {

    private static final List<String> MVP_ROLES = List.of("owner", "admin", "member", "guest");

    @GetMapping("/api/me")
    @Operation(
            summary = "Get the authenticated caller profile",
            description = "Returns the canonical Weave identity and profile snapshot available to the backend.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated caller details.",
                    content = @Content(schema = @Schema(implementation = AuthenticatedUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Bearer token is missing the weave:workspace scope.",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        String subject = jwt.getSubject();
        String username = firstText(jwt.getClaimAsString("preferred_username"), subject);
        String displayName = firstText(jwt.getClaimAsString("name"), username);
        String email = jwt.getClaimAsString("email");
        List<String> realmRoles = extractRealmRoles(jwt);
        List<String> groups = extractStringList(jwt, "groups");
        return new AuthenticatedUserResponse(
                subject,
                username,
                email,
                emailVerified(jwt),
                displayName,
                firstText(jwt.getClaimAsString("locale"), "en"),
                firstText(jwt.getClaimAsString("timezone"), "UTC"),
                productRoles(realmRoles),
                groups,
                issuedFor(jwt),
                jwt.getAudience(),
                realmRoles,
                new ModuleSyncStatusResponse("not_configured", "not_configured"));
    }

    private String issuedFor(Jwt jwt) {
        return Stream.of(jwt.getClaimAsString("azp"), jwt.getClaimAsString("client_id"))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> roleValues)) {
            return List.of();
        }

        return roleValues.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .sorted()
                .toList();
    }

    private List<String> extractStringList(Jwt jwt, String claimName) {
        Object claim = jwt.getClaims().get(claimName);
        if (!(claim instanceof Collection<?> values)) {
            return List.of();
        }

        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .sorted()
                .toList();
    }

    private boolean emailVerified(Jwt jwt) {
        Object value = jwt.getClaims().get("email_verified");
        return value instanceof Boolean verified && verified;
    }

    private List<String> productRoles(List<String> realmRoles) {
        return realmRoles.stream()
                .map(role -> role.toLowerCase(Locale.ROOT))
                .filter(MVP_ROLES::contains)
                .distinct()
                .sorted()
                .toList();
    }

    private String firstText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }
}
