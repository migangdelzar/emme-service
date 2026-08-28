package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantScopedDataSourceTest {

  private final DataSource delegate = mock(DataSource.class);
  private final TenantIdentifierResolver schemaResolver = mock(TenantIdentifierResolver.class);

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void setsTheAuthenticatedTenantSchemaBeforeReturningAConnection() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    when(delegate.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(schemaResolver.resolveCurrentTenantIdentifier()).thenReturn("e2e_studio");

    TenantContext.setCurrentTenant(tenantId);

    Connection scoped = new TenantScopedDataSource(delegate, schemaResolver).getConnection();

    assertThat(scoped).isSameAs(connection);
    verify(schemaResolver).resolveCurrentTenantIdentifier();
    verify(connection).setSchema("e2e_studio");
    verify(statement).execute("SET search_path TO e2e_studio, emme_core, public");
  }

  @Test
  void refusesToOpenAConnectionWithoutAnAuthenticatedTenant() {
    TenantScopedDataSource dataSource = new TenantScopedDataSource(delegate, schemaResolver);

    assertThatThrownBy(dataSource::getConnection)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No tenant context");
    verifyNoInteractions(delegate, schemaResolver);
  }
}
