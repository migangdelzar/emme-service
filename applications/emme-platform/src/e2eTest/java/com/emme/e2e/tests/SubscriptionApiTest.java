package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubscriptionApiTest {

  private static final String DEMO_TENANT = "00000000-0000-0000-0000-100000000000";

  @Test
  void shouldGetSubscriptionForTenant() {
    withSession(
        s -> {
          var result = s.subscriptions().get(DEMO_TENANT);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldEnforceEntitlement() {
    withSession(
        s -> {
          String body =
              """
                {"entitlement":"customers:write"}
                """;
          var result = s.post("/api/subscriptions/" + DEMO_TENANT + "/enforce", body, 200);
          assertThat(result).isNotNull();
        });
  }
}
