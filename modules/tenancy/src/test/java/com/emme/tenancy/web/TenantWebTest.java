package com.emme.tenancy.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseWebTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * L2 web slice tests for TenantController HTTP contracts. Extends {@link BaseWebTest} — provides
 * MockMvc with full Spring Security. Uses H2 database via the "web" profile.
 */
@DisplayName("Tenant Web")
class TenantWebTest extends BaseWebTest {

  @Test
  @DisplayName("POST /api/v1/tenants with empty body → 400 Bad Request (missing slug, name)")
  void shouldReturn400ForMissingFields() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tenants")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/tenants with valid slug and name → 201 Created")
  void shouldAcceptValidTenant() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tenants")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"slug":"web-test-tenant","name":"Web Test Salon"}"""))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.slug").value("web-test-tenant"))
        .andExpect(jsonPath("$.name").value("Web Test Salon"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("POST /api/v1/tenants with slug longer than 50 characters → 400 Bad Request")
  void shouldRejectOversizedSlug() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tenants")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"slug":"%s","name":"Web Test Salon"}"""
                        .formatted("s".repeat(51))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/v1/tenants/{unknownId} → 404 Not Found")
  void shouldReturn404ForUnknownTenant() throws Exception {
    UUID unknownId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/tenants/" + unknownId).with(auth()))
        .andExpect(status().isNotFound());
  }
}
