package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessConfigApiTest {

  @Test
  void shouldGetProfile() {
    withSession(
        s -> {
          var result = s.get("/api/business-config/profile", 404);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldGetHours() {
    withSession(
        s -> {
          var result = s.businessConfig().hours();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldGetPolicy() {
    withSession(
        s -> {
          var result = s.get("/api/business-config/policy", 404);
          assertThat(result).isNotNull();
        });
  }
}
