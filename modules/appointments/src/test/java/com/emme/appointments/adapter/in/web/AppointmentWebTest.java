package com.emme.appointments.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AppointmentWebTest extends BaseWebTest {

  @Autowired private SpringDataSubscriptionRepository subscriptionRepo;

  private UUID artistId;
  private UUID customerId;
  private UUID serviceId;

  @BeforeEach
  void setUp() throws Exception {
    TenantDetails tenant = createTenant("web-test-" + System.nanoTime(), "Web Test Salon");
    tenantId = tenant.id();
    subscriptionRepo.save(
        new SubscriptionEntity(
            tenantId, PlanType.ENTERPRISE, Instant.now().plus(365, ChronoUnit.DAYS)));

    // Create prerequisite entities via API
    MvcResult artistResult =
        mockMvc
            .perform(
                post("/api/artists")
                    .with(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Web Artist\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    artistId = extractId(artistResult);

    MvcResult customerResult =
        mockMvc
            .perform(
                post("/api/customers")
                    .with(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Web Customer\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    customerId = extractId(customerResult);

    MvcResult serviceResult =
        mockMvc
            .perform(
                post("/api/services")
                    .with(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\"web-svc\",\"name\":\"Web Service\",\"durationMinutes\":30,\"basePrice\":10.00}"))
            .andExpect(status().isCreated())
            .andReturn();
    serviceId = extractId(serviceResult);
  }

  @Test
  void shouldReturn400ForMissingFields() throws Exception {
    mockMvc
        .perform(
            post("/api/appointments")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn404ForUnknownAppointment() throws Exception {
    mockMvc
        .perform(get("/api/appointments/{id}", UUID.randomUUID()).with(auth()))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldAcceptValidAppointment() throws Exception {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);

    String body =
        String.format(
            "{\"customerId\":\"%s\",\"serviceId\":\"%s\",\"artistId\":\"%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}",
            customerId, serviceId, artistId, startsAt.toString(), endsAt.toString());

    mockMvc
        .perform(
            post("/api/appointments")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists());
  }

  private static UUID extractId(MvcResult result) throws Exception {
    String json = result.getResponse().getContentAsString();
    int start = json.indexOf("\"id\":\"") + 6;
    int end = json.indexOf("\"", start);
    return UUID.fromString(json.substring(start, end));
  }
}
