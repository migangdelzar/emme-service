package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentitySecurityPropertiesTest {

  @Test
  void providesSafeDefaultsForLocalDevelopment() {
    IdentitySecurityProperties properties = IdentitySecurityProperties.defaults();

    assertThat(properties.allowedOrigins())
        .containsExactly(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8100",
            "capacitor://localhost");
    assertThat(properties.allowedHeaders())
        .containsExactly("Authorization", "Content-Type", "X-Tenant-Id");
    assertThat(properties.allowCredentials()).isTrue();
    assertThat(properties.maxAgeSeconds()).isEqualTo(3600L);
  }
}
