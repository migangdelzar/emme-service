package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityKeycloakPropertiesTest {

  @Test
  void exposesTypedUserAndAdminClientSettings() {
    IdentityKeycloakProperties properties =
        new IdentityKeycloakProperties(
            "http://localhost:18080",
            "http://localhost:18080/realms/emme",
            "http://keycloak:8080",
            "salon-app",
            "admin-app",
            "master",
            "admin",
            "secret",
            "emme",
            "http://localhost:18080/realms/emme-customers",
            "client-app");

    assertThat(properties.baseUrl()).isEqualTo("http://localhost:18080");
    assertThat(properties.issuerUri()).isEqualTo("http://localhost:18080/realms/emme");
    assertThat(properties.jwkSetBaseUrl()).isEqualTo("http://keycloak:8080");
    assertThat(properties.clientId()).isEqualTo("salon-app");
    assertThat(properties.platformClientId()).isEqualTo("admin-app");
    assertThat(properties.adminRealm()).isEqualTo("master");
    assertThat(properties.adminUsername()).isEqualTo("admin");
    assertThat(properties.adminPassword()).isEqualTo("secret");
    assertThat(properties.defaultRealm()).isEqualTo("emme");
    assertThat(properties.customerIssuerUri())
        .isEqualTo("http://localhost:18080/realms/emme-customers");
    assertThat(properties.customerClientId()).isEqualTo("client-app");
  }
}
