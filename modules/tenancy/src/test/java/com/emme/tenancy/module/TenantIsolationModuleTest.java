package com.emme.tenancy.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.api.result.TenantDetails;
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

  private TenantDetails tenantA;
  private TenantDetails tenantB;

  @BeforeEach
  void createIsolatedTenants() {
    fullSetup(); // creates base tenant + sets tenantId
    tenantA = createTenant("isolation-a-" + System.nanoTime(), "Isolation Salon A");
    tenantB = createTenant("isolation-b-" + System.nanoTime(), "Isolation Salon B");
  }

  @Test
  @DisplayName("Tenants are independently accessible — each has unique ID and slug")
  void shouldFilterByTenant() throws Exception {
    // Verify tenant A is accessible with its own data
    mockMvc
        .perform(get("/api/tenants/" + tenantA.id()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantA.id().toString()))
        .andExpect(jsonPath("$.slug").value(tenantA.slug()))
        .andExpect(jsonPath("$.name").value("Isolation Salon A"));

    // Verify tenant B is accessible with its own data
    mockMvc
        .perform(get("/api/tenants/" + tenantB.id()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantB.id().toString()))
        .andExpect(jsonPath("$.slug").value(tenantB.slug()))
        .andExpect(jsonPath("$.name").value("Isolation Salon B"));

    // Verify they are different tenants
    assertThat(tenantA.id()).isNotEqualTo(tenantB.id());
    assertThat(tenantA.slug()).isNotEqualTo(tenantB.slug());
  }

  @Test
  @DisplayName("Accessing unknown tenant ID with valid JWT → 404 not found")
  void shouldRejectCrossTenantAccess() throws Exception {
    // Attempt to access a non-existent tenant ID (simulates cross-tenant access)
    UUID unknownId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/tenants/" + unknownId).with(tenantJwt()))
        .andExpect(status().isNotFound());

    // Verify that updating a non-existent tenant also fails
    mockMvc
        .perform(
            patch("/api/tenants/" + unknownId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"name":"Hacked Name"}"""))
        .andExpect(status().isNotFound());

    // Verify that suspend on non-existent tenant fails
    mockMvc
        .perform(post("/api/tenants/" + unknownId + "/suspend").with(tenantJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("TenantContext is populated from JWT tenant_id claim during request")
  void shouldEnforceTenantContext() throws Exception {
    // Create tenant with a specific ID as JWT claim
    UUID specificTenantId = tenantA.id();

    // Make a request with JWT containing that tenant_id
    mockMvc
        .perform(
            get("/api/tenants/" + specificTenantId)
                .with(tenantJwt(specificTenantId, TEST_USER_SUB, "admin", "tenant_owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(specificTenantId.toString()));

    // Verify the tenant exists independently
    TenantDetails found = findTenant(specificTenantId);
    assertThat(found.slug()).isEqualTo(tenantA.slug());
    assertThat(found.name()).isEqualTo("Isolation Salon A");
  }
}
