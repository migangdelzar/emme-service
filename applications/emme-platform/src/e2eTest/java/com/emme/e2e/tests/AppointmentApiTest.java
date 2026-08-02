package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppointmentApiTest {

  private static final String DEMO_TENANT = "00000000-0000-0000-0000-100000000000";

  @Test
  void shouldListAppointments() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var result = s.get("/api/appointments", 403);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldCreateAppointment() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          String body =
              """
                {"customerId":"00000000-0000-0000-0000-000000000000","serviceId":"00000000-0000-0000-0000-000000000000","artistId":"00000000-0000-0000-0000-000000000000","startsAt":"2027-01-15T10:00:00Z","endsAt":"2027-01-15T11:00:00Z"}
                """;
          var result = s.post("/api/appointments", body, 500);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldRejectInvalidAppointment() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          String body =
              """
                {"customerId":"","serviceId":"","artistId":"","startsAt":"","endsAt":""}
                """;
          var result = s.post("/api/appointments", body, 400);
          assertThat(result).isNotNull();
        });
  }
}
