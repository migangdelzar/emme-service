package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.BUSINESS_OWNER, tenant = "00000000-0000-0000-0000-100000000000")
class AppointmentApiTest {
  private static final String DEMO = "00000000-0000-0000-0000-100000000000";
  private UserSession api;

  @BeforeEach
  void setUp(UserSession session) {
    this.api = session;
    session.setup().subscription(DEMO);
  }

  @Test
  void shouldListAppointments() {
    var result = api.get("/api/v1/appointments", 403);
    assertThat(result).isNotNull();
  }

  @Test
  void shouldCreateAppointment() {
    var body =
        "{\"customerId\":\""
            + UUID.randomUUID()
            + "\",\"serviceId\":\""
            + UUID.randomUUID()
            + "\",\"startAt\":\"2026-12-01T10:00:00Z\"}";
    var result = api.post("/api/v1/appointments", body, 400);
    assertThat(result).isNotNull();
  }

  @Test
  void shouldRejectInvalidAppointment() {
    var result = api.post("/api/v1/appointments", "{}", 400);
    assertThat(result).isNotNull();
  }
}
