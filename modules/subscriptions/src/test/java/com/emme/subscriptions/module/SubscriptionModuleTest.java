package com.emme.subscriptions.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.tenancy.testing.BaseTenantModuleTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SubscriptionModuleTest extends BaseTenantModuleTest {

  @BeforeEach
  void setUp() {
    fullSetup();
  }

  @Test
  void shouldGetSubscription() throws Exception {
    mockMvc
        .perform(get("/api/subscriptions/{tenantId}", tenantId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan").value("ENTERPRISE"))
        .andExpect(jsonPath("$.status").value("TRIAL"));
  }

  @Test
  void shouldChangePlan() throws Exception {
    mockMvc
        .perform(
            put("/api/subscriptions/{tenantId}/plan", tenantId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"STARTER\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan").value("STARTER"))
        .andExpect(jsonPath("$.status").value("TRIAL"));
  }

  @Test
  void shouldEnforceEntitlement() throws Exception {
    mockMvc
        .perform(
            post("/api/subscriptions/{tenantId}/enforce", tenantId)
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entitlement\":\"ai:basic\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectWithoutJwt() throws Exception {
    mockMvc
        .perform(get("/api/subscriptions/{tenantId}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }
}
