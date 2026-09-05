package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.util.HashMap;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;

class SchemaMultiTenantConnectionProviderTest {

  @Test
  void getsCoreConnectionsFromTheMetadataDataSource() throws Exception {
    DataSource metadataDataSource = mock(DataSource.class);
    TenantDatabasePoolProvider tenantPools = mock(TenantDatabasePoolProvider.class);
    Connection connection = mock(Connection.class);
    when(metadataDataSource.getConnection()).thenReturn(connection);

    SchemaMultiTenantConnectionProvider provider =
        new SchemaMultiTenantConnectionProvider(metadataDataSource, tenantPools);

    assertThat(provider.getConnection("emme_core")).isSameAs(connection);
    verifyNoInteractions(tenantPools);
  }

  @Test
  void getsTenantConnectionsFromThePoolAndSelectsTheRequestedSchema() throws Exception {
    DataSource metadataDataSource = mock(DataSource.class);
    TenantDatabasePoolProvider tenantPools = mock(TenantDatabasePoolProvider.class);
    HikariDataSource tenantDataSource = mock(HikariDataSource.class);
    Connection connection = mock(Connection.class);
    when(tenantPools.getDataSource()).thenReturn(tenantDataSource);
    when(tenantDataSource.getConnection()).thenReturn(connection);

    SchemaMultiTenantConnectionProvider provider =
        new SchemaMultiTenantConnectionProvider(metadataDataSource, tenantPools);

    assertThat(provider.getConnection("tenant_schema")).isSameAs(connection);
    verify(connection).setSchema("tenant_schema");
  }

  @Test
  void registersTheSpringManagedProviderInstanceWithHibernate() {
    DataSource metadataDataSource = mock(DataSource.class);
    TenantDatabasePoolProvider tenantPools = mock(TenantDatabasePoolProvider.class);
    SchemaMultiTenantConnectionProvider provider =
        new SchemaMultiTenantConnectionProvider(metadataDataSource, tenantPools);
    var hibernateProperties = new HashMap<String, Object>();

    provider.customize(hibernateProperties);

    assertThat(hibernateProperties)
        .containsEntry(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, provider);
  }
}
