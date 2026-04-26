package com.massimotter.weave.backend.controller;

import com.massimotter.weave.backend.config.ApiAccessDeniedHandler;
import com.massimotter.weave.backend.config.ApiAuthenticationEntryPoint;
import com.massimotter.weave.backend.config.ApiErrorResponseWriter;
import com.massimotter.weave.backend.config.SecurityConfig;
import com.massimotter.weave.backend.exception.ApiExceptionHandler;
import com.massimotter.weave.backend.service.CalendarFacadeService;
import com.massimotter.weave.backend.service.FilesFacadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {FilesController.class, CalendarController.class},
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiErrorResponseWriter.class,
        ApiExceptionHandler.class,
        FilesFacadeService.class,
        CalendarFacadeService.class
})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave"
})
class FilesCalendarFacadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void filesFacadeRequiresAuthenticatedWorkspaceScope() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void filesFacadeExposesStableUnavailableErrorUntilNextcloudAdapterExists() throws Exception {
        mockMvc.perform(get("/api/files")
                        .with(workspaceJwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("nextcloud-adapter-not-configured"))
                .andExpect(jsonPath("$.message").value(
                        "Files facade is available, but the downstream Nextcloud adapter is not configured yet."))
                .andExpect(jsonPath("$.details.module").value("files"))
                .andExpect(jsonPath("$.details.operation").value("list-files"));
    }

    @Test
    void calendarFacadeRequiresAuthenticatedWorkspaceScope() throws Exception {
        mockMvc.perform(get("/api/calendar/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void calendarFacadeExposesStableUnavailableErrorUntilNextcloudAdapterExists() throws Exception {
        mockMvc.perform(get("/api/calendar/events")
                        .with(workspaceJwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("nextcloud-adapter-not-configured"))
                .andExpect(jsonPath("$.message").value(
                        "Calendar facade is available, but the downstream Nextcloud adapter is not configured yet."))
                .andExpect(jsonPath("$.details.module").value("calendar"))
                .andExpect(jsonPath("$.details.operation").value("list-events"));
    }

    @Test
    void facadeRequestsUseStableValidationEnvelope() throws Exception {
        String invalidEvent = """
                {
                  "title": "",
                  "startsAt": "2026-04-26T10:00:00+02:00",
                  "endsAt": "2026-04-26T09:00:00+02:00",
                  "timezone": "Europe/Berlin"
                }
                """;

        mockMvc.perform(post("/api/calendar/events")
                        .with(workspaceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEvent))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("validation-error"))
                .andExpect(jsonPath("$.details.fields.title").exists())
                .andExpect(jsonPath("$.details.fields.timeRangeValid").exists());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor workspaceJwt() {
        return jwt().jwt(jwt -> jwt
                        .subject("user-123")
                        .claim("aud", java.util.List.of("weave-app")))
                .authorities(new SimpleGrantedAuthority("SCOPE_weave:workspace"));
    }
}
