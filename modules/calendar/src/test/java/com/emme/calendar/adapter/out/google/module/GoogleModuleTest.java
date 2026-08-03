package com.emme.calendar.adapter.out.google.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
      "app.google.oauth.client-id=test-client-id",
      "app.google.oauth.client-secret=test-client-secret",
      "app.google.oauth.redirect-uri=http://localhost:8080/api/google/oauth/callback",
      "app.google.oauth.encryption-key=12345678901234567890123456789012" // 32 bytes for AES-256
    })
class GoogleModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void statusReturnsDisconnectedWhenNoTokenExists() throws Exception {
    mockMvc
        .perform(get("/api/google/oauth/status").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(false));
  }

  @Test
  void statusRejectsWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/google/oauth/status")).andExpect(status().isUnauthorized());
  }

  @Test
  void authorizeRedirectsToGoogle() throws Exception {
    mockMvc
        .perform(get("/api/google/oauth/authorize").with(tenantJwt()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void authorizeRejectsWithoutJwt() throws Exception {
    mockMvc.perform(get("/api/google/oauth/authorize")).andExpect(status().isUnauthorized());
  }
}
