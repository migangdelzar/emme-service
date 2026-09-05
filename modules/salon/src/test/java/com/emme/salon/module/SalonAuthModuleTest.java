package com.emme.salon.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.BaseTenantModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalonAuthModuleTest extends BaseTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldAccessWithAdminJwt() throws Exception {
    // tenantJwt() provides admin + tenant_owner
    mockMvc.perform(get("/api/artists").with(tenantJwt())).andExpect(status().isOk());
  }

  @Test
  void shouldAccessWithTenantJwt() throws Exception {
    // Tenant-only JWT should be able to read non-protected endpoints
    mockMvc
        .perform(get("/api/artists").with(tenantJwt(tenantId, TEST_USER_SUB, "tenant_owner")))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectInvalidRole() throws Exception {
    // GET /api/appointments requires admin authority
    mockMvc
        .perform(get("/api/appointments").with(tenantJwt(tenantId, TEST_USER_SUB, "customer")))
        .andExpect(status().isForbidden());
  }
}
