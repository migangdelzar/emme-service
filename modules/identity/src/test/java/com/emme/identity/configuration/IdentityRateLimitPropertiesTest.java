package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityRateLimitPropertiesTest {

  @Test
  void providesSecureDefaults() {
    IdentityRateLimitProperties properties = new IdentityRateLimitProperties();

    assertThat(properties.getMaxAttempts()).isEqualTo(5);
    assertThat(properties.getWindowMs()).isEqualTo(60_000L);
    assertThat(properties.getTrustedProxies()).isEmpty();
  }

  @Test
  void copiesConfiguredTrustedProxyNetworks() {
    IdentityRateLimitProperties properties = new IdentityRateLimitProperties();

    properties.setTrustedProxies(List.of("10.0.0.0/8"));

    assertThat(properties.getTrustedProxies()).containsExactly("10.0.0.0/8");
  }

  @Test
  void rejectsNonPositiveLimits() {
    IdentityRateLimitProperties properties = new IdentityRateLimitProperties();

    assertThatThrownBy(() -> properties.setMaxAttempts(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> properties.setWindowMs(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
