package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TenantDataSourceConfigurationTest {

  @Test
  void exposesTheTenantScopedDataSourceAsAStableInfrastructureBoundary() {
    DataSource routingDataSource = mock(DataSource.class);

    DataSource scopedDataSource =
        new TenantDataSourceConfiguration().tenantScopedDataSource(routingDataSource);

    assertThat(scopedDataSource).isInstanceOf(TenantScopedDataSource.class);
  }
}
