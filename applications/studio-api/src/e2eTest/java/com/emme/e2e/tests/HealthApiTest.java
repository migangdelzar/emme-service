package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.PLATFORM_ADMIN)
class HealthApiTest {

  @Test
  void shouldGetHealthCheck(UserSession api) {
    var result = api.get("/actuator/health");
    assertThat(result).isNotNull().contains("UP");
  }

  @Test
  void shouldGetLiveness(UserSession api) {
    var result = api.get("/actuator/health/liveness");
    assertThat(result).isNotNull();
  }

  @Test
  void shouldGetReadiness(UserSession api) {
    var result = api.get("/actuator/health/readiness");
    assertThat(result).isNotNull();
  }

  @Test
  void shouldGetCurrentUser(UserSession api) {
    var result = api.get("/api/me", 200);
    assertThat(result).isNotNull();
  }
}
