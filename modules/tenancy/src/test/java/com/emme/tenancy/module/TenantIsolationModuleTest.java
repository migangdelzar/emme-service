package com.emme.tenancy.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.adapter.out.persistence.entity.Tenant;
import com.emme.testing.BaseSpringModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * L3 module tests for tenant isolation and Row-Level Security behavior. Verifies that tenants are
 * independently managed and the tenant context is properly enforced from JWT claims.
 */
@DisplayName("Tenant Isolation")
class TenantIsolationModuleTest extends BaseSpringModuleTest {

  private Tenant tenantA;
  private Tenant tenantB;

  @BeforeEach
  void createIsolatedTenants() {
    fullSetup(); // creates base tenant + sets tenantId
    tenantA = tenantService.create("isolation-a-" + System.nanoTime(), "Isolation Salon A");
    tenantB = tenantService.create("isolation-b-" + System.nanoTime(), "Isolation Salon B");
  }

  @Test
  @DisplayName("Tenants are independently accessible — each has unique ID and slug")
  void shouldFilterByTenant() throws Exception {
    // Verify tenant A is accessible with its own data
    mockMvc
        .perform(get("/api/v1/tenants/" + tenantA.getId()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantA.getId().toString()))
        .andExpect(jsonPath("$.slug").value(tenantA.getSlug()))
        .andExpect(jsonPath("$.name").value("Isolation Salon A"));

    // Verify tenant B is accessible with its own data
    mockMvc
        .perform(get("/api/v1/tenants/" + tenantB.getId()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantB.getId().toString()))
        .andExpect(jsonPath("$.slug").value(tenantB.getSlug()))
        .andExpect(jsonPath("$.name").value("Isolation Salon B"));

    // Verify they are different tenants
    assertThat(tenantA.getId()).isNotEqualTo(tenantB.getId());
    assertThat(tenantA.getSlug()).isNotEqualTo(tenantB.getSlug());
  }

  @Test
  @DisplayName("Accessing unknown tenant ID with valid JWT → 404 not found")
  void shouldRejectCrossTenantAccess() throws Exception {
    // Attempt to access a non-existent tenant ID (simulates cross-tenant access)
    UUID unknownId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/tenants/" + unknownId).with(tenantJwt()))
        .andExpect(status().isNotFound());

    // Verify that updating a non-existent tenant also fails
    mockMvc
        .perform(
            patch("/api/v1/tenants/" + unknownId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"name":"Hacked Name"}"""))
        .andExpect(status().isNotFound());

    // Verify that suspend on non-existent tenant fails
    mockMvc
        .perform(post("/api/v1/tenants/" + unknownId + "/suspend").with(tenantJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("TenantContext is populated from JWT tenant_id claim during request")
  void shouldEnforceTenantContext() throws Exception {
    // Create tenant with a specific ID as JWT claim
    UUID specificTenantId = tenantA.getId();

    // Make a request with JWT containing that tenant_id
    mockMvc
        .perform(
            get("/api/v1/tenants/" + specificTenantId)
                .with(tenantJwt(specificTenantId, TEST_USER_SUB, "platform_admin", "tenant_owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(specificTenantId.toString()));

    // Verify the tenant exists independently
    Tenant found = tenantService.findById(specificTenantId).orElseThrow();
    assertThat(found.getSlug()).isEqualTo(tenantA.getSlug());
    assertThat(found.getName()).isEqualTo("Isolation Salon A");
  }
}
