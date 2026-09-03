package com.emme.assistant.adapter.in.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.assistant.ai.adapter.in.web.security.AiPrincipalIdentity;
import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.testing.BaseWebTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ConversationWebTest extends BaseWebTest {

  @Autowired private SpringDataSubscriptionRepository subscriptionRepo;

  @BeforeEach
  void setUp() {
    TenantDetails tenant = createTenant("conv-web-" + System.nanoTime(), "Conv Web Tenant");
    tenantId = tenant.id();
    subscriptionRepo.save(
        new SubscriptionEntity(
            tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
  }

  @Test
  void shouldRejectUnauthenticatedRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"participantId\":\"" + UUID.randomUUID() + "\",\"channel\":\"WHATSAPP\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldAcceptValidConversationRequest() throws Exception {
    UUID callerControlledParticipant = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/conversations")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"participantId\":\""
                        + callerControlledParticipant
                        + "\",\"channel\":\"WEB_CHAT\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(
            jsonPath("$.participantId")
                .value(
                    AiPrincipalIdentity.fromTrustedClaims(
                            "https://issuer.example/emme-test", "test-user")
                        .toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}
