package com.massimotter.weave.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.invalid/realms/weave"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void exposesOpenApiDescription() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Weave Backend API"))
                .andExpect(jsonPath("$.paths['/api/me']").exists())
                .andExpect(jsonPath("$.paths['/api/health/live']").exists())
                .andExpect(jsonPath("$.paths['/api/health/ready']").exists())
                .andExpect(jsonPath("$.paths['/api/platform/config']").exists())
                .andExpect(jsonPath("$.paths['/api/platform/status']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/workspace/capabilities']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/workspace/release-readiness']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.code.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.message.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.requestId.type").value("string"))
                .andExpect(jsonPath("$.components.responses.UnauthorizedError.description").value("Missing or invalid bearer token."))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].type").value("http"));
    }
}
