package com.emme.studio.documents.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.entity.Subscription;
import com.emme.studio.subscriptions.entity.SubscriptionRepository;
import com.emme.tenancy.application.TenantService;
import com.emme.testing.BaseWebTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DocumentWebTest extends BaseWebTest {

  @Autowired private TenantService tenantService;

  @Autowired private SubscriptionRepository subscriptionRepo;

  @BeforeEach
  void setUp() {
    var tenant = tenantService.create("doc-web-" + System.nanoTime(), "Doc Web Tenant");
    tenantId = tenant.getId();
    subscriptionRepo.save(
        new Subscription(tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
  }

  @Test
  void shouldReturnBadRequestForEmptyUpload() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/documents")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sourceType\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Doc\",\"sourceType\":\"PDF\"}"))
        .andExpect(status().isUnauthorized());
  }
}
