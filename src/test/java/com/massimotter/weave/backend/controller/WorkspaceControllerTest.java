package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.config.ApiAccessDeniedHandler;
import com.massimotter.weave.backend.config.ApiAuthenticationEntryPoint;
import com.massimotter.weave.backend.config.ApiErrorResponseWriter;
import com.massimotter.weave.backend.config.SecurityConfig;
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
        controllers = WorkspaceController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import({SecurityConfig.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class, ApiErrorResponseWriter.class})
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave"
})
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsStaticWorkspaceCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/capabilities").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_weave:workspace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shellAccess.enabled").value(true))
                .andExpect(jsonPath("$.shellAccess.readiness").value("ready"))
                .andExpect(jsonPath("$.chat.readiness").value("ready"))
                .andExpect(jsonPath("$.files.readiness").value("ready"))
                .andExpect(jsonPath("$.calendar.enabled").value(false))
                .andExpect(jsonPath("$.calendar.readiness").value("unavailable"))
                .andExpect(jsonPath("$.boards.readiness").value("unavailable"));
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/capabilities"))
                .andExpect(status().isUnauthorized());
    }
}
