package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityApiTest {

  @Test
  void shouldGetCurrentUser() {
    withSession(
        s -> {
          var result = s.identity().me();
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldGetIdentityMe() {
    withSession(
        s -> {
          var result = s.identity().identityMe();
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldDenyFeatureFlagsWithoutAdmin() {
    withSession(
        s -> {
          var result = s.get("/api/admin/feature-flags", 403);
          assertThat(result).isNotNull();
        });
  }
}
