package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TenantSchemaNameTest {

  @Test
  void acceptsLowercaseSchemaIdentifiers() {
    assertThat(TenantSchemaName.requireValid("studio_a")).isEqualTo("studio_a");
  }

  @Test
  void rejectsSqlFragmentsAndUnexpectedIdentifiers() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TenantSchemaName.requireValid("studio_a\"; DROP SCHEMA public;--"))
        .withMessageContaining("Invalid tenant schema name");
  }
}
