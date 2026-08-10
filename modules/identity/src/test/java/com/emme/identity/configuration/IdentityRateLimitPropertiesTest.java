package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityRateLimitPropertiesTest {

  @Test
  void providesSecureDefaults() {
    IdentityRateLimitProperties properties = IdentityRateLimitProperties.defaults();

    assertThat(properties.maxAttempts()).isEqualTo(5);
    assertThat(properties.windowMs()).isEqualTo(60_000L);
    assertThat(properties.trustedProxies()).isEmpty();
  }

  @Test
  void copiesConfiguredTrustedProxyNetworks() {
    IdentityRateLimitProperties properties =
        new IdentityRateLimitProperties(5, 60_000L, List.of("10.0.0.0/8"));

    assertThat(properties.trustedProxies()).containsExactly("10.0.0.0/8");
  }

  @Test
  void rejectsNonPositiveLimits() {
    assertThatThrownBy(() -> new IdentityRateLimitProperties(0, 60_000L, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new IdentityRateLimitProperties(5, 0L, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
