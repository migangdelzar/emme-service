package com.emme.documents.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

class DocumentWebTest extends BaseWebTest {

  @Autowired private SpringDataSubscriptionRepository subscriptionRepo;

  @BeforeEach
  void setUp() {
    TenantDetails tenant = createTenant("doc-web-" + System.nanoTime(), "Doc Web Tenant");
    tenantId = tenant.id();
    subscriptionRepo.save(
        new SubscriptionEntity(
            tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
  }

  @Test
  void shouldReturnBadRequestForEmptyUpload() throws Exception {
    mockMvc
        .perform(
            post("/api/documents")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sourceType\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Doc\",\"sourceType\":\"PDF\"}"))
        .andExpect(status().isUnauthorized());
  }
}
