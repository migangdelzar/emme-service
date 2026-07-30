package com.emme.identity.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** L3 module tests for feature flag endpoints (admin CRUD, tenant overrides, auth gating). */
class FeatureFlagModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldListGlobalFeatureFlags() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/feature-flags").with(tenantJwt()))
        .andExpect(status().isOk());
  }

  @Test
  void shouldCreateAndUpdateFeatureFlag() throws Exception {
    String code = "test-flag-" + System.nanoTime();

    // Create
    String createBody =
        """
                {
                    "code": "%s",
                    "enabled": true,
                    "planRequired": "ENTERPRISE"
                }
                """
            .formatted(code);

    mockMvc
        .perform(
            post("/api/v1/admin/feature-flags")
                .with(tenantJwt())
                .contentType("application/json")
                .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.enabled").value(true));

    // Update — disable it
    String updateBody =
        """
                {
                    "enabled": false,
                    "planRequired": "PRO"
                }
                """;

    mockMvc
        .perform(
            put("/api/v1/admin/feature-flags/{code}", code)
                .with(tenantJwt())
                .contentType("application/json")
                .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void shouldRejectNonAdminForFeatureFlags() throws Exception {
    mockMvc.perform(get("/api/v1/admin/feature-flags")).andExpect(status().is4xxClientError());
  }
}
