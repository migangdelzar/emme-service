package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentitySecurityPropertiesTest {

  @Test
  void providesSafeDefaultsForLocalDevelopment() {
    IdentitySecurityProperties properties = new IdentitySecurityProperties();

    assertThat(properties.getAllowedOrigins())
        .containsExactly(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8100",
            "capacitor://localhost");
    assertThat(properties.getAllowedHeaders())
        .containsExactly("Authorization", "Content-Type", "X-Tenant-Id");
    assertThat(properties.isAllowCredentials()).isTrue();
    assertThat(properties.getMaxAgeSeconds()).isEqualTo(3600L);
  }
}
