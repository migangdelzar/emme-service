package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthApiTest {

  @Test
  void shouldReportHealthUp() {
    withUnauthenticated(
        s -> {
          var result = s.get("/actuator/health");
          assertThat(result).contains("UP");
        });
  }

  @Test
  void shouldExposeMetrics() {
    withUnauthenticated(
        s -> {
          var result = s.get("/actuator/metrics", 401);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldServeApiDocs() {
    withUnauthenticated(
        s -> {
          var result = s.get("/api-docs");
          assertThat(result).contains("openapi");
        });
  }

  @Test
  void shouldGetCurrentUser() {
    withSession(
        s -> {
          var result = s.identity().me();
          assertThat(result).isNotNull();
        });
  }
}
