package com.emme.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityKeycloakPropertiesTest {

  @Test
  void exposesTypedUserAndAdminClientSettings() {
    IdentityKeycloakProperties properties = new IdentityKeycloakProperties();

    properties.setBaseUrl("http://localhost:18080");
    properties.setIssuerUri("http://localhost:18080/realms/emme");
    properties.setClientId("emme-salon-app");
    properties.setAdminRealm("master");
    properties.setAdminUsername("admin");
    properties.setAdminPassword("secret");
    properties.setDefaultRealm("emme");
    properties.setCustomerIssuerUri("http://localhost:18080/realms/emme-customers");
    properties.setCustomerClientId("emme-customer-app");

    assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:18080");
    assertThat(properties.getIssuerUri()).isEqualTo("http://localhost:18080/realms/emme");
    assertThat(properties.getClientId()).isEqualTo("emme-salon-app");
    assertThat(properties.getAdminRealm()).isEqualTo("master");
    assertThat(properties.getAdminUsername()).isEqualTo("admin");
    assertThat(properties.getAdminPassword()).isEqualTo("secret");
    assertThat(properties.getDefaultRealm()).isEqualTo("emme");
    assertThat(properties.getCustomerIssuerUri())
        .isEqualTo("http://localhost:18080/realms/emme-customers");
    assertThat(properties.getCustomerClientId()).isEqualTo("emme-customer-app");
  }
}
