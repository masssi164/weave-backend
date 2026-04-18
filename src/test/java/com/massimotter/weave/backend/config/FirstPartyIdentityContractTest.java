package com.massimotter.weave.backend.config;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave",
        "weave.security.required-audience=weave-backend"
})
@AutoConfigureMockMvc
class FirstPartyIdentityContractTest {

    private static final String REQUIRED_AUDIENCE = "weave-backend";
    private static final String FIRST_PARTY_CLIENT_ID = "com.massimotter.weave";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void configureJwtDecoder() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> decode(invocation.getArgument(0)));
    }

    @Test
    void acceptsTokenWithRequiredAudience() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/capabilities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer correct-audience"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-audience", "missing-audience"})
    void rejectsTokensWithoutRequiredAudience(String tokenValue) throws Exception {
        mockMvc.perform(get("/api/v1/workspace/capabilities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutWorkspaceScope() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/capabilities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer missing-scope"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsValidFullFirstPartyToken() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-full-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user-123"))
                .andExpect(jsonPath("$.preferredUsername").value("alice"))
                .andExpect(jsonPath("$.issuedFor").value(FIRST_PARTY_CLIENT_ID))
                .andExpect(jsonPath("$.audience[0]").value(REQUIRED_AUDIENCE))
                .andExpect(jsonPath("$.realmRoles[0]").value("member"));
    }

    private Jwt decode(String tokenValue) {
        Jwt jwt = switch (tokenValue) {
            case "correct-audience" -> jwt(tokenValue, List.of(REQUIRED_AUDIENCE), "weave:workspace", null);
            case "wrong-audience" -> jwt(tokenValue, List.of("other-api"), "weave:workspace", null);
            case "missing-audience" -> jwt(tokenValue, null, "weave:workspace", null);
            case "missing-scope" -> jwt(tokenValue, List.of(REQUIRED_AUDIENCE), "openid profile", null);
            case "valid-full-token" -> jwt(tokenValue, List.of(REQUIRED_AUDIENCE), "openid profile weave:workspace",
                    Map.of(
                            "preferred_username", "alice",
                            "name", "Alice Example",
                            "email", "alice@example.com",
                            "azp", FIRST_PARTY_CLIENT_ID,
                            "client_id", FIRST_PARTY_CLIENT_ID,
                            "realm_access", Map.of("roles", List.of("member"))));
            default -> throw new JwtException("Unknown test token.");
        };

        OAuth2TokenValidatorResult audienceValidation =
                JwtDecoderConfig.requiredAudienceValidator(REQUIRED_AUDIENCE).validate(jwt);
        if (audienceValidation.hasErrors()) {
            throw new JwtValidationException("JWT missing required audience.", audienceValidation.getErrors());
        }

        return jwt;
    }

    private Jwt jwt(String tokenValue, List<String> audience, String scope, Map<String, Object> claims) {
        Instant now = Instant.parse("2026-04-18T12:00:00Z");
        Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer("https://auth.example.invalid/realms/weave")
                .subject("user-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("scope", scope);

        if (audience != null) {
            builder.audience(audience);
        }
        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.build();
    }
}
