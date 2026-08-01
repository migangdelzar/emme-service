package com.emme.identity.adapter.in.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseWebTest;
import org.junit.jupiter.api.Test;

/** L2 web slice tests for auth controller HTTP contracts. */
class AuthWebTest extends BaseWebTest {

  @Test
  void shouldReturnServerErrorForLoginWithoutKeycloak() throws Exception {
    // Keycloak not available in test → authenticate() throws IOException → 500
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@test.com\",\"password\":\"wrong\"}"))
        .andExpect(status().is5xxServerError());
  }

  @Test
  void shouldRejectUnauthenticatedCurrentUserRequest() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().is4xxClientError());
  }
}
