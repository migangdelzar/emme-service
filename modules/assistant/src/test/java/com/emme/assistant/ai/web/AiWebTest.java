package com.emme.assistant.ai.adapter.in.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.adapter.out.persistence.repository.SpringDataFeatureFlagRepository;
import com.emme.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.testing.BaseWebTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AiWebTest extends BaseWebTest {

  @Autowired private SpringDataSubscriptionRepository subscriptionRepo;

  @Autowired private SpringDataFeatureFlagRepository featureFlagRepo;

  @BeforeEach
  void setUp() {
    TenantDetails tenant = createTenant("ai-web-" + System.nanoTime(), "AI Web Tenant");
    tenantId = tenant.id();
    subscriptionRepo.save(
        new SubscriptionEntity(
            tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
    featureFlagRepo.save(new FeatureFlagEntity(null, "ai_chat", true, null, "global"));
  }

  @Test
  void shouldRejectWithoutFeatureFlag() throws Exception {
    // Delete flag so pre-authorize fails
    featureFlagRepo.deleteAll();
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"Hello\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAcceptValidChatRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userMessage\":\"Tell me about pricing\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.response").exists());
  }
}
