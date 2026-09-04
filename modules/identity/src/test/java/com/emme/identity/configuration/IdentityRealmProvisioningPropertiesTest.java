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
    assertThat(properties.initialAdminRole()).isEqualTo("tenant_owner");
    assertThat(properties.initialOwnerUsername()).isEqualTo("owner");
    assertThat(properties.initialOwnerPassword()).isBlank();
    assertThat(properties.initialOwnerRole()).isEqualTo("tenant_owner");
    assertThat(properties.defaultRoles()).containsExactly("tenant_owner", "tenant_staff");
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
            "tenant_owner",
            "owner",
            "owner-password",
            "tenant_owner",
            List.of("tenant_owner", "tenant_staff"),
            3,
            2_000L);

    assertThat(properties.redirectUris()).containsExactly("https://studio.example/*");
  }
}
