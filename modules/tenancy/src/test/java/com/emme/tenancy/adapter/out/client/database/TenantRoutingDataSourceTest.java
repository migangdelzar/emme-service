package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRoutingDataSourceTest {

  private TenantPoolingProperties poolingProperties = TenantPoolingProperties.defaults();
  private final TenantDatabasePoolProvider poolProvider = mock(TenantDatabasePoolProvider.class);

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void routesToTheConfiguredDefaultDatabaseWithoutTenantContext() {
    UUID defaultDatabaseId = UUID.randomUUID();
    poolingProperties =
        new TenantPoolingProperties(200, 30, 60, 5, 20, 100, defaultDatabaseId.toString());
    TenantRoutingDataSource routingDataSource =
        new TenantRoutingDataSource(poolProvider, poolingProperties);

    assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(defaultDatabaseId);
  }

  @Test
  void routesToTheDatabaseResolvedForTheCurrentTenant() {
    UUID databaseId = UUID.randomUUID();
    TenantContext.setCurrentDatabaseId(databaseId);
    TenantRoutingDataSource routingDataSource =
        new TenantRoutingDataSource(poolProvider, poolingProperties);

    assertThat(routingDataSource.determineCurrentLookupKey()).isEqualTo(databaseId);
  }

  @Test
  void delegatesTargetResolutionToTheLazyPoolProvider() {
    HikariDataSource expectedDataSource = mock(HikariDataSource.class);
    when(poolProvider.getDataSource()).thenReturn(expectedDataSource);
    TenantRoutingDataSource routingDataSource =
        new TenantRoutingDataSource(poolProvider, poolingProperties);

    assertThat(routingDataSource.determineTargetDataSource()).isSameAs(expectedDataSource);
    verify(poolProvider).getDataSource();
  }
}
