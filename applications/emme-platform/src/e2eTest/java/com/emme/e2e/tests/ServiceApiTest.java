package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceApiTest {

  private static final String DEMO_TENANT = "00000000-0000-0000-0000-100000000000";

  @Test
  void shouldListServices() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var result = s.services().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateService() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          String uniqueName = UUID.randomUUID().toString().substring(0, 8) + "-E2E-Svc";
          var result =
              s.services()
                  .create(uniqueName, "E2E-" + uniqueName.substring(0, 10), 500, 60, "Manicura");
          assertThat(result).isNotNull().contains("\"name\":\"" + uniqueName + "\"");
        });
  }

  @Test
  void shouldFilterByCategory() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var result = s.services().listByCategory("Manicura");
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldRejectEmptyFields() {
    withSession(
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var result =
              s.post(
                  "/api/services",
                  "{\"code\":\"\",\"name\":\"\",\"basePrice\":null,\"durationMinutes\":0,\"category\":\"\"}",
                  400);
          assertThat(result).isNotNull();
        });
  }
}
