package com.emme.identity.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.EntitledTenantModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L3 module tests for auth flows (login, current user, unauthenticated access). Tests the full
 * Spring context with H2, MockMvc, and JWT auth.
 */
class AuthModuleTest extends EntitledTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldReturnCurrentUserWithValidJwt() throws Exception {
    mockMvc
        .perform(get("/api/me").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").exists())
        .andExpect(jsonPath("$.email").exists());
  }

  @Test
  void shouldRejectLoginWithEmptyBody() throws Exception {
    mockMvc
        .perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectUnauthenticatedAccessToProtectedEndpoint() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().is4xxClientError());
  }
}
