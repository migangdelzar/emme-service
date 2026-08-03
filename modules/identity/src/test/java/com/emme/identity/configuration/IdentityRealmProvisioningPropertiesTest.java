package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityRealmProvisioningPropertiesTest {

  @Test
  void providesSafeProvisioningDefaultsWithoutAProvisioningPassword() {
    IdentityRealmProvisioningProperties properties = new IdentityRealmProvisioningProperties();

    assertThat(properties.getClientId()).isEqualTo("emme-salon-app");
    assertThat(properties.getRedirectUris())
        .containsExactly("http://localhost:8080/*", "http://localhost:3000/*");
    assertThat(properties.getInitialAdminUsername()).isEqualTo("admin");
    assertThat(properties.getInitialAdminPassword()).isBlank();
    assertThat(properties.getInitialAdminRole()).isEqualTo("business_owner");
    assertThat(properties.getDefaultRoles())
        .containsExactly("business_owner", "nail_artist", "front_desk", "read_only");
    assertThat(properties.getMaxAttempts()).isEqualTo(3);
    assertThat(properties.getRetryDelayMillis()).isEqualTo(2_000L);
  }

  @Test
  void copiesConfiguredRedirectUris() {
    IdentityRealmProvisioningProperties properties = new IdentityRealmProvisioningProperties();

    properties.setRedirectUris(List.of("https://studio.example/*"));

    assertThat(properties.getRedirectUris()).containsExactly("https://studio.example/*");
  }
}
