package com.emme.studio.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.studio.subscriptions.adapter.out.persistence.entity.SubscriptionEntity;
import com.emme.studio.subscriptions.adapter.out.persistence.repository.SpringDataSubscriptionRepository;
import com.emme.studio.subscriptions.api.type.PlanType;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.testing.BaseWebTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class CustomerWebTest extends BaseWebTest {

  @Autowired private SpringDataSubscriptionRepository subscriptionRepo;

  @BeforeEach
  void setUp() {
    TenantInfo tenant = createTenant("web-cust-" + System.nanoTime(), "Web Customer Tenant");
    tenantId = tenant.id();
    subscriptionRepo.save(
        new SubscriptionEntity(
            tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));
  }

  @Test
  void shouldReturn400ForBlankName() throws Exception {
    // CreateCustomerRequest requires @NotBlank name — empty name triggers 400
    mockMvc
        .perform(
            post("/api/customers")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldAcceptValidCustomer() throws Exception {
    mockMvc
        .perform(
            post("/api/customers")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Valid Customer\",\"phone\":\"555-0100\",\"email\":\"valid@example.com\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Valid Customer"));
  }
}
