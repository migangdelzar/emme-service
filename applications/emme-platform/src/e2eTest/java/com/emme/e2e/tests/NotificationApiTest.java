package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationApiTest {

  @Test
  void shouldListNotifications() {
    withSession(
        s -> {
          var result = s.notifications().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateNotification() {
    withSession(
        s -> {
          var result = s.notifications().send("EMAIL", "e2e-test@emme.app", "Hello from E2E");
          assertThat(result).isNotNull().contains("\"channel\"");
        });
  }
}
