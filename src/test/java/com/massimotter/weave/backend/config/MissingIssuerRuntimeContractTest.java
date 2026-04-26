package com.massimotter.weave.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MissingIssuerRuntimeContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithoutIssuerAndFailsProtectedRoutesClosed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me")
                .header(AUTHORIZATION, "Bearer invalid-without-issuer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.details.status").value(401))
                .andExpect(jsonPath("$.message").value(
                        "Bearer authentication is required and must satisfy the first-party Weave token contract."));
    }
}
