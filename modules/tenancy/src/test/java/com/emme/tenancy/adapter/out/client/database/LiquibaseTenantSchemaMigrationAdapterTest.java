package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class LiquibaseTenantSchemaMigrationAdapterTest {

  private final DataSource dataSource = mock(DataSource.class);
  private final LiquibaseTenantSchemaMigrationAdapter adapter =
      new LiquibaseTenantSchemaMigrationAdapter(dataSource);

  @Test
  void rejectsUnsafeSchemaNamesBeforeOpeningAConnection() {
    assertThatThrownBy(() -> adapter.migrate("studio_a; DROP SCHEMA public"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid tenant schema name");

    verifyNoInteractions(dataSource);
  }
}
