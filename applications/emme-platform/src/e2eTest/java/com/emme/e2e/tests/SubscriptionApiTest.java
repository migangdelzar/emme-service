package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubscriptionApiTest {

  @Test
  void shouldGetSubscriptionForTenant() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          var result = s.subscriptions().get(s.tenantId());
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldEnforceEntitlement() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          String body =
              """
                {"entitlement":"customers:write"}
                """;
          var result = s.post("/api/subscriptions/" + s.tenantId() + "/enforce", body, 200);
          assertThat(result).isNotNull();
        });
  }
}
