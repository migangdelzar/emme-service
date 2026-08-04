package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import com.emme.tenancy.configuration.TenantDatabaseConnectionProperties;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
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

  @Test
  void replacesAClosedDefaultPoolOnTheNextLookup() {
    UUID defaultDatabaseId = UUID.fromString(poolingProperties.getDefaultDatabaseId());
    when(registry.findById(defaultDatabaseId))
        .thenReturn(
            Optional.of(
                new DatabaseRegistryEntry(
                    defaultDatabaseId,
                    "default",
                    "jdbc:h2:mem:default-pool-recovery",
                    0,
                    1,
                    0,
                    true)));
    poolingProperties.setDefaultMinPoolSize(0);
    poolingProperties.setDefaultMaxPoolSize(1);
    connectionProperties.setDriverClassName("org.h2.Driver");

    TenantDatabasePoolProvider provider =
        new TenantDatabasePoolProvider(poolingProperties, connectionProperties, registry);

    var firstPool = provider.getDataSource();
    firstPool.close();

    var recoveredPool = provider.getDataSource();

    assertThat(recoveredPool).isNotSameAs(firstPool);
    assertThat(recoveredPool.isClosed()).isFalse();

    provider.shutdown();
  }

  @Test
  void evictsIdleTenantPoolsAndClosesTheirResources() {
    UUID tenantDatabaseId = UUID.randomUUID();
    when(registry.findById(tenantDatabaseId))
        .thenReturn(
            Optional.of(
                new DatabaseRegistryEntry(
                    tenantDatabaseId,
                    "studio-a",
                    "jdbc:h2:mem:idle-pool-eviction",
                    0,
                    1,
                    0,
                    true)));
    poolingProperties.setIdleTimeoutMinutes(1);
    poolingProperties.setDefaultMinPoolSize(0);
    poolingProperties.setDefaultMaxPoolSize(1);
    connectionProperties.setDriverClassName("org.h2.Driver");
    ManualTicker ticker = new ManualTicker();

    TenantDatabasePoolProvider provider =
        new TenantDatabasePoolProvider(poolingProperties, connectionProperties, registry, ticker);
    TenantContext.setCurrentDatabaseId(tenantDatabaseId);

    var firstPool = provider.getDataSource();
    ticker.advance(Duration.ofMinutes(2));
    var replacementPool = provider.getDataSource();

    assertThat(replacementPool).isNotSameAs(firstPool);
    assertThat(firstPool.isClosed()).isTrue();

    provider.shutdown();
  }

  private static final class ManualTicker implements Ticker {

    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    void advance(Duration duration) {
      nanos += duration.toNanos();
    }
  }
}
