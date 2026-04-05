package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.model.AuthenticatedUserResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new AuthenticatedUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("azp"),
                jwt.getAudience(),
                extractRealmRoles(jwt),
                extractStringList(jwt, "groups"));
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
}
