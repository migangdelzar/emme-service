package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.tenancy.adapter.out.client.database.TenantIdentifierResolver;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TenantDataSourceConfigurationTest {

  @Test
  void exposesTheTenantScopedDataSourceAsAStableInfrastructureBoundary() {
    DataSource routingDataSource = mock(DataSource.class);
    TenantIdentifierResolver resolver = mock(TenantIdentifierResolver.class);

    DataSource scopedDataSource =
        new TenantDataSourceConfiguration().tenantScopedDataSource(routingDataSource, resolver);

    assertThat(scopedDataSource).isInstanceOf(TenantScopedDataSource.class);
  }
}
