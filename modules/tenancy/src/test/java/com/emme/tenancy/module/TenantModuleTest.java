package com.emme.tenancy.module;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.adapter.out.persistence.entity.Tenant;
import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * L3 module tests for Tenant CRUD via HTTP layer. Extends {@link BaseSpringModuleTest} — provides
 * MockMvc, full Spring context, H2 database.
 */
@DisplayName("Tenant Module")
class TenantModuleTest extends BaseSpringModuleTest {

  @Test
  @DisplayName("POST /api/v1/tenants → 201 with location header and tenant fields")
  void shouldCreateTenant() throws Exception {
    fullSetup(); // sets tenantId for tenantJwt()

    mockMvc
        .perform(
            post("/api/v1/tenants")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"slug":"test-tenant-1","name":"Test Salon One"}"""))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.slug").value("test-tenant-1"))
        .andExpect(jsonPath("$.name").value("Test Salon One"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  @DisplayName("GET /api/v1/tenants → 200 with list of all tenants")
  void shouldListTenants() throws Exception {
    fullSetup(); // ensures we have at least one tenant

    // Create two test tenants via service for predictable slugs
    Tenant t1 = tenantService.create("test-tenant-a-" + System.nanoTime(), "Salon Alpha");
    Tenant t2 = tenantService.create("test-tenant-b-" + System.nanoTime(), "Salon Beta");

    mockMvc
        .perform(get("/api/v1/tenants").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$[*].id", hasItems(t1.getId().toString(), t2.getId().toString())))
        .andExpect(jsonPath("$[*].slug", hasItems(t1.getSlug(), t2.getSlug())));
  }

  @Test
  @DisplayName("GET /api/v1/tenants/{id} → 200 with correct tenant data")
  void shouldGetTenantById() throws Exception {
    fullSetup();
    Tenant tenant = tenantService.create("test-tenant-2-" + System.nanoTime(), "Test Salon Two");

    mockMvc
        .perform(get("/api/v1/tenants/" + tenant.getId()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
        .andExpect(jsonPath("$.slug").value(tenant.getSlug()))
        .andExpect(jsonPath("$.name").value(tenant.getName()))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("PATCH /api/v1/tenants/{id} → 200 updates tenant name")
  void shouldUpdateTenant() throws Exception {
    fullSetup();
    Tenant tenant = tenantService.create("test-tenant-3-" + System.nanoTime(), "Original Name");

    mockMvc
        .perform(
            patch("/api/v1/tenants/" + tenant.getId())
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"name":"Updated Salon Name"}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
        .andExpect(jsonPath("$.name").value("Updated Salon Name"));
  }

  @Test
  @DisplayName("POST /api/v1/tenants without JWT → 401 Unauthorized")
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"slug":"no-auth-tenant","name":"No Auth"}"""))
        .andExpect(status().isUnauthorized());
  }
}
