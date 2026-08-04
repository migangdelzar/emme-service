package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantDatabaseConnectionPropertiesTest {

  @Test
  void providesSafeLocalDefaultsForTenantPoolCreation() {
    TenantDatabaseConnectionProperties properties = TenantDatabaseConnectionProperties.defaults();

    assertThat(properties.url()).isEmpty();
    assertThat(properties.username()).isEqualTo("emme");
    assertThat(properties.password()).isEqualTo("emme");
    assertThat(properties.driverClassName()).isEqualTo("org.postgresql.Driver");
  }

  @Test
  void exposesTheConnectionSettingsAsACompleteTypedValue() {
    TenantDatabaseConnectionProperties properties =
        new TenantDatabaseConnectionProperties(
            "jdbc:postgresql://localhost/emme",
            "tenant-user",
            "tenant-password",
            "org.postgresql.Driver");

    assertThat(properties.url()).isEqualTo("jdbc:postgresql://localhost/emme");
    assertThat(properties.username()).isEqualTo("tenant-user");
    assertThat(properties.password()).isEqualTo("tenant-password");
    assertThat(properties.driverClassName()).isEqualTo("org.postgresql.Driver");
  }
}
