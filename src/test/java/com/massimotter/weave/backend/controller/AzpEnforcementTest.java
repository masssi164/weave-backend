package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests azp caller-binding enforcement when WEAVE_OIDC_ALLOWED_AZP is configured.
 *
 * <p>The allowlist is set to {@code weave-app} for the "enforcement enabled" tests.
 * Existing tests in {@link IdentityControllerTest} and {@link WorkspaceControllerTest}
 * cover the default (empty allowlist = enforcement disabled) path.
 */
@WebMvcTest(controllers = IdentityController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave",
        "weave.security.allowed-azp=weave-app"
})
class AzpEnforcementTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void permitsTokenWithAllowedAzp() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwt -> jwt
                        .claim("scope", "openid profile")
                        .claim("azp", "weave-app"))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsTokenWithDisallowedAzp() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwt -> jwt
                        .claim("scope", "openid profile")
                        .claim("azp", "evil-client"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithAbsentAzpClaim() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwt -> jwt
                        .claim("scope", "openid profile"))))
                .andExpect(status().isUnauthorized());
    }
}
