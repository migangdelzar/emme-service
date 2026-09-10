package com.emme.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class UserSessionTest {

  @Test
  void usesFixtureTenantWhenTheTokenDoesNotContainATenantClaim() {
    try (var session =
        new UserSession(URI.create("http://localhost"), E2eUserPool.newTestUser(0), "")) {
      assertThat(session.tenantId()).isNotBlank();
    }
  }
}
