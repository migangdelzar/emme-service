package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityRealmProvisioningPropertiesTest {

  @Test
  void providesSafeProvisioningDefaultsWithoutAProvisioningPassword() {
    IdentityRealmProvisioningProperties properties = IdentityRealmProvisioningProperties.defaults();

    assertThat(properties.clientId()).isEqualTo("salon-app");
    assertThat(properties.redirectUris())
        .containsExactly("http://localhost:8080/*", "http://localhost:3000/*");
    assertThat(properties.initialAdminUsername()).isEqualTo("admin");
    assertThat(properties.initialAdminPassword()).isBlank();
    assertThat(properties.initialAdminRole()).isEqualTo("business_owner");
    assertThat(properties.defaultRoles())
        .containsExactly("business_owner", "nail_artist", "front_desk", "read_only");
    assertThat(properties.maxAttempts()).isEqualTo(3);
    assertThat(properties.retryDelayMillis()).isEqualTo(2_000L);
  }

  @Test
  void copiesConfiguredRedirectUris() {
    IdentityRealmProvisioningProperties properties =
        new IdentityRealmProvisioningProperties(
            "salon-app",
            List.of("https://studio.example/*"),
            "admin",
            "",
            "business_owner",
            List.of("business_owner", "nail_artist", "front_desk", "read_only"),
            3,
            2_000L);

    assertThat(properties.redirectUris()).containsExactly("https://studio.example/*");
  }
}
