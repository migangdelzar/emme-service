package com.emme.assistant.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.entity.Subscription;
import com.emme.studio.subscriptions.entity.SubscriptionRepository;
import com.emme.tenancy.application.TenantService;
import com.emme.testing.BaseWebTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ConversationWebTest extends BaseWebTest {

  @Autowired private TenantService tenantService;

  @Autowired private SubscriptionRepository subscriptionRepo;

  @BeforeEach
  void setUp() {
    var tenant = tenantService.create("conv-web-" + System.nanoTime(), "Conv Web Tenant");
    tenantId = tenant.getId();
    subscriptionRepo.save(
        new Subscription(tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
  }

  @Test
  void shouldRejectUnauthenticatedRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"participantId\":\"" + UUID.randomUUID() + "\",\"channel\":\"WHATSAPP\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldAcceptValidConversationRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/conversations")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"participantId\":\"" + UUID.randomUUID() + "\",\"channel\":\"WHATSAPP\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}
