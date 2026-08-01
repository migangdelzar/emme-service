package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantDatabaseConnectionPropertiesTest {

  @Test
  void providesSafeLocalDefaultsForTenantPoolCreation() {
    TenantDatabaseConnectionProperties properties = new TenantDatabaseConnectionProperties();

    assertThat(properties.getUsername()).isEqualTo("emme");
    assertThat(properties.getPassword()).isEqualTo("emme");
    assertThat(properties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
  }

  @Test
  void exposesTheConnectionSettingsAsACompleteTypedValue() {
    TenantDatabaseConnectionProperties properties = new TenantDatabaseConnectionProperties();

    properties.setUsername("tenant-user");
    properties.setPassword("tenant-password");
    properties.setDriverClassName("org.postgresql.Driver");

    assertThat(properties.getUsername()).isEqualTo("tenant-user");
    assertThat(properties.getPassword()).isEqualTo("tenant-password");
    assertThat(properties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
  }
}
