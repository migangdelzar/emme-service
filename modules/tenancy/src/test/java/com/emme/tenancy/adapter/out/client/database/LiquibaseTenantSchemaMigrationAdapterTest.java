package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.shared.persistence.jdbc.BootstrapConnectionExecutor;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiquibaseTenantSchemaMigrationAdapterTest {

  private final BootstrapConnectionExecutor connectionExecutor =
      mock(BootstrapConnectionExecutor.class);
  private final LiquibaseTenantSchemaMigrationAdapter adapter =
      new LiquibaseTenantSchemaMigrationAdapter(connectionExecutor);

  @Test
  void rejectsEmptySchemaNamesBeforeOpeningAConnection() {
    assertThatThrownBy(() -> adapter.migrate(UUID.randomUUID(), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid tenant schema name");

    verifyNoInteractions(connectionExecutor);
  }

  @Test
  void executesMigrationThroughTheConnectionExecutor() {
    adapter.migrate(UUID.randomUUID(), "studio_a");

    verify(connectionExecutor).consumeWithConnection(any());
  }
}
