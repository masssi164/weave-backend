package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.config.ApiAccessDeniedHandler;
import com.massimotter.weave.backend.config.ApiAuthenticationEntryPoint;
import com.massimotter.weave.backend.config.ApiErrorResponseWriter;
import com.massimotter.weave.backend.config.PlatformContractProperties;
import com.massimotter.weave.backend.config.SecurityConfig;
import com.massimotter.weave.backend.config.WeaveSecurityProperties;
import com.massimotter.weave.backend.config.WorkspaceCapabilityProperties;
import com.massimotter.weave.backend.service.PlatformContractService;
import com.massimotter.weave.backend.service.WorkspaceCapabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {PlatformController.class, HealthController.class},
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        PlatformContractService.class,
        WorkspaceCapabilityService.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiErrorResponseWriter.class
})
@EnableConfigurationProperties({
        PlatformContractProperties.class,
        WeaveSecurityProperties.class,
        WorkspaceCapabilityProperties.class,
        OAuth2ResourceServerProperties.class
})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.weave.local/realms/weave",
        "weave.workspace.chat.dependency-url=https://matrix.weave.local",
        "weave.workspace.files.dependency-url=https://files.weave.local",
        "weave.workspace.calendar.enabled=true"
})
class PlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void exposesPublicPlatformConfig() throws Exception {
        mockMvc.perform(get("/api/platform/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicBaseUrl").value("https://weave.local"))
                .andExpect(jsonPath("$.apiBaseUrl").value("https://weave.local/api"))
                .andExpect(jsonPath("$.authBaseUrl").value("https://auth.weave.local"))
                .andExpect(jsonPath("$.matrixBaseUrl").value("https://matrix.weave.local"))
                .andExpect(jsonPath("$.filesProductUrl").value("https://weave.local/files"))
                .andExpect(jsonPath("$.calendarProductUrl").value("https://weave.local/calendar"))
                .andExpect(jsonPath("$.nextcloudRawBaseUrl").value("https://files.weave.local"))
                .andExpect(jsonPath("$.targets.mobile").value(true))
                .andExpect(jsonPath("$.targets.desktop").value(true))
                .andExpect(jsonPath("$.targets.web").value(false))
                .andExpect(jsonPath("$.features.chat").value(true))
                .andExpect(jsonPath("$.features.chatE2ee").value(false))
                .andExpect(jsonPath("$.features.matrixFederation").value(false))
                .andExpect(jsonPath("$.features.files").value(true))
                .andExpect(jsonPath("$.features.calendar").value(true));
    }

    @Test
    void exposesPublicPlatformStatus() throws Exception {
        mockMvc.perform(get("/api/platform/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend.status").value("up"))
                .andExpect(jsonPath("$.auth.status").value("up"))
                .andExpect(jsonPath("$.matrix.status").value("up"))
                .andExpect(jsonPath("$.matrix.federationEnabled").value(false))
                .andExpect(jsonPath("$.matrix.e2eeEnabled").value(false))
                .andExpect(jsonPath("$.nextcloud.status").value("up"));
    }

    @Test
    void exposesPublicHealthEndpoints() throws Exception {
        mockMvc.perform(get("/api/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("up"));

        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("up"));
    }
}
