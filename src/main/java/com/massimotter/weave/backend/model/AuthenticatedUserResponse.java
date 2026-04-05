package com.massimotter.weave.backend.model;

import java.util.List;

public record AuthenticatedUserResponse(
        String sub,
        String preferredUsername,
        String name,
        String email,
        String issuedFor,
        List<String> audience,
        List<String> realmRoles,
        List<String> groups) {
}
