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

import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.testing.EntitledTenantModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * L3 module tests for Tenant CRUD via HTTP layer. Extends {@link EntitledTenantModuleTest} —
 * provides MockMvc, full Spring context, H2 database.
 */
@DisplayName("Tenant Module")
class TenantModuleTest extends EntitledTenantModuleTest {

  @Test
  @DisplayName("POST /api/tenants → 201 with location header and tenant fields")
  void shouldCreateTenant() throws Exception {
    fullSetup(); // sets tenantId for tenantJwt()

    mockMvc
        .perform(
            post("/api/tenants")
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
  @DisplayName("GET /api/tenants → 200 with list of all tenants")
  void shouldListTenants() throws Exception {
    fullSetup(); // ensures we have at least one tenant

    // Create two test tenants via service for predictable slugs
    TenantDetails t1 = createTenant("test-tenant-a-" + System.nanoTime(), "Salon Alpha");
    TenantDetails t2 = createTenant("test-tenant-b-" + System.nanoTime(), "Salon Beta");

    mockMvc
        .perform(get("/api/tenants").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$[*].id", hasItems(t1.id().toString(), t2.id().toString())))
        .andExpect(jsonPath("$[*].slug", hasItems(t1.slug(), t2.slug())));
  }

  @Test
  @DisplayName("GET /api/tenants/{id} → 200 with correct tenant data")
  void shouldGetTenantById() throws Exception {
    fullSetup();
    TenantDetails tenant = createTenant("test-tenant-2-" + System.nanoTime(), "Test Salon Two");

    mockMvc
        .perform(get("/api/tenants/" + tenant.id()).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenant.id().toString()))
        .andExpect(jsonPath("$.slug").value(tenant.slug()))
        .andExpect(jsonPath("$.name").value(tenant.name()))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("PATCH /api/tenants/{id} → 200 updates tenant name")
  void shouldUpdateTenant() throws Exception {
    fullSetup();
    TenantDetails tenant = createTenant("test-tenant-3-" + System.nanoTime(), "Original Name");

    mockMvc
        .perform(
            patch("/api/tenants/" + tenant.id())
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"name":"Updated Salon Name"}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenant.id().toString()))
        .andExpect(jsonPath("$.name").value("Updated Salon Name"));
  }

  @Test
  @DisplayName("POST /api/tenants without JWT → 401 Unauthorized")
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"slug":"no-auth-tenant","name":"No Auth"}"""))
        .andExpect(status().isUnauthorized());
  }
}
