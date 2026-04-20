package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.config.SecurityConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IdentityController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import(SecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave"
})
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsAuthenticatedPrincipalDetails() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwt -> jwt
                        .subject("user-123")
                        .claim("preferred_username", "alice")
                        .claim("name", "Alice Example")
                        .claim("email", "alice@example.com")
                        .claim("azp", "weave-app")
                        .claim("aud", List.of("weave-app", "account"))
                        .claim("realm_access", Map.of("roles", List.of("member", "admin")))
                        .claim("groups", List.of("team-alpha", "team-beta")))
                        .authorities(new SimpleGrantedAuthority("SCOPE_weave:workspace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user-123"))
                .andExpect(jsonPath("$.preferredUsername").value("alice"))
                .andExpect(jsonPath("$.issuedFor").value("weave-app"))
                .andExpect(jsonPath("$.audience[0]").value("weave-app"))
                .andExpect(jsonPath("$.realmRoles[0]").value("admin"))
                .andExpect(jsonPath("$.groups[1]").value("team-beta"));
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }
}
