package com.emme.studio.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalonAuthModuleTest extends BaseSpringModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldAccessWithAdminJwt() throws Exception {
    // tenantJwt() provides platform_admin + tenant_owner
    mockMvc.perform(get("/api/v1/artists").with(tenantJwt())).andExpect(status().isOk());
  }

  @Test
  void shouldAccessWithTenantJwt() throws Exception {
    // Tenant-only JWT should be able to read non-protected endpoints
    mockMvc
        .perform(get("/api/v1/artists").with(tenantJwt(tenantId, TEST_USER_SUB, "tenant_owner")))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectInvalidRole() throws Exception {
    // GET /api/v1/appointments requires platform_admin authority
    mockMvc
        .perform(get("/api/v1/appointments").with(tenantJwt(tenantId, TEST_USER_SUB, "customer")))
        .andExpect(status().isForbidden());
  }
}
