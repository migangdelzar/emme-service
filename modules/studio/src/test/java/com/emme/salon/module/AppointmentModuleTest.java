package com.emme.studio.module;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emme.testing.BaseSpringModuleTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AppointmentModuleTest extends BaseSpringModuleTest {

  private UUID artistId;
  private UUID customerId;
  private UUID serviceId;

  @BeforeEach
  void setUp() {
    fullSetup();

    // Create prerequisite entities via API
    try {
      // Create artist
      MvcResult artistResult =
          mockMvc
              .perform(
                  post("/api/v1/artists")
                      .with(tenantJwt())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"name\":\"Test Artist\"}"))
              .andExpect(status().isCreated())
              .andReturn();
      artistId = extractId(artistResult);

      // Create customer
      MvcResult customerResult =
          mockMvc
              .perform(
                  post("/api/v1/customers")
                      .with(tenantJwt())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"name\":\"Test Customer\",\"phone\":\"555-0001\",\"email\":\"test@example.com\"}"))
              .andExpect(status().isCreated())
              .andReturn();
      customerId = extractId(customerResult);

      // Create service
      MvcResult serviceResult =
          mockMvc
              .perform(
                  post("/api/v1/services")
                      .with(tenantJwt())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"code\":\"cut\",\"name\":\"Haircut\",\"durationMinutes\":30,\"basePrice\":25.00}"))
              .andExpect(status().isCreated())
              .andReturn();
      serviceId = extractId(serviceResult);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set up test data", e);
    }
  }

  @Test
  void shouldCreateAppointment() throws Exception {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);

    String body =
        String.format(
            "{\"customerId\":\"%s\",\"serviceId\":\"%s\",\"artistId\":\"%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}",
            customerId, serviceId, artistId, startsAt.toString(), endsAt.toString());

    mockMvc
        .perform(
            post("/api/v1/appointments")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void shouldListAppointments() throws Exception {
    // Create an appointment first so the list is non-empty
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
    String body =
        String.format(
            "{\"customerId\":\"%s\",\"serviceId\":\"%s\",\"artistId\":\"%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}",
            customerId, serviceId, artistId, startsAt.toString(), endsAt.toString());

    mockMvc
        .perform(
            post("/api/v1/appointments")
                .with(tenantJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/appointments").with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").isNotEmpty());
  }

  @Test
  void shouldGetAppointmentById() throws Exception {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
    String body =
        String.format(
            "{\"customerId\":\"%s\",\"serviceId\":\"%s\",\"artistId\":\"%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}",
            customerId, serviceId, artistId, startsAt.toString(), endsAt.toString());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/appointments")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    UUID appointmentId = extractId(result);

    mockMvc
        .perform(get("/api/v1/appointments/{id}", appointmentId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(appointmentId.toString()));
  }

  @Test
  void shouldCancelAppointment() throws Exception {
    Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
    Instant endsAt = startsAt.plus(1, ChronoUnit.HOURS);
    String body =
        String.format(
            "{\"customerId\":\"%s\",\"serviceId\":\"%s\",\"artistId\":\"%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}",
            customerId, serviceId, artistId, startsAt.toString(), endsAt.toString());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/appointments")
                    .with(tenantJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    UUID appointmentId = extractId(result);

    mockMvc
        .perform(post("/api/v1/appointments/{id}/cancel", appointmentId).with(tenantJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(post("/api/v1/appointments").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  private static UUID extractId(MvcResult result) throws Exception {
    String json = result.getResponse().getContentAsString();
    int start = json.indexOf("\"id\":\"") + 6;
    int end = json.indexOf("\"", start);
    return UUID.fromString(json.substring(start, end));
  }
}
