package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import com.emme.tenancy.configuration.TenantDatabaseConnectionProperties;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantDatabasePoolProviderTest {

  private final TenantPoolingProperties poolingProperties = new TenantPoolingProperties();
  private final TenantDatabaseConnectionProperties connectionProperties =
      new TenantDatabaseConnectionProperties();
  private final DatabaseRegistryPort registry = mock(DatabaseRegistryPort.class);

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void reportsAnEmptyPoolLifecycleBeforeAndAfterShutdown() {
    TenantDatabasePoolProvider provider =
        new TenantDatabasePoolProvider(poolingProperties, connectionProperties, registry);

    assertThat(provider.getActivePoolCount()).isZero();

    provider.shutdown();

    assertThat(provider.getActivePoolCount()).isZero();
  }

  @Test
  void failsWithTheDatabaseIdWhenRegistryLookupCannotResolveTheDefaultPool() {
    UUID defaultDatabaseId = UUID.fromString(poolingProperties.getDefaultDatabaseId());
    when(registry.findById(defaultDatabaseId)).thenReturn(Optional.empty());
    TenantDatabasePoolProvider provider =
        new TenantDatabasePoolProvider(poolingProperties, connectionProperties, registry);

    assertThatThrownBy(provider::getDataSource)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(defaultDatabaseId.toString());

    provider.shutdown();
  }
}
