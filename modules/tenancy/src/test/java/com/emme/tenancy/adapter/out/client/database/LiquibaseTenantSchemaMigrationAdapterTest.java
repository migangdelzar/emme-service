package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import org.junit.jupiter.api.Test;

class LiquibaseTenantSchemaMigrationAdapterTest {

  private final JdbcConnectionExecutor connectionExecutor = mock(JdbcConnectionExecutor.class);
  private final LiquibaseTenantSchemaMigrationAdapter adapter =
      new LiquibaseTenantSchemaMigrationAdapter(connectionExecutor);

  @Test
  void rejectsUnsafeSchemaNamesBeforeOpeningAConnection() {
    assertThatThrownBy(() -> adapter.migrate("studio_a; DROP SCHEMA public"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid tenant schema name");

    verifyNoInteractions(connectionExecutor);
  }

  @Test
  void executesMigrationThroughTheConnectionExecutor() {
    adapter.migrate("studio_a");

    verify(connectionExecutor).consumeWithConnection(any());
  }
}
