package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class TenantIdentifierResolverTest {

  @Test
  void resolvesTenantSchemaUsingTheBootstrapJdbcClient() {
    UUID tenantId = UUID.randomUUID();
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> result = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
    when(statement.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(statement);
    when(statement.query(String.class)).thenReturn(result);
    when(result.single()).thenReturn("tenant_schema");

    TenantContext.setCurrentTenant(tenantId);

    assertThat(new TenantIdentifierResolver(jdbc).resolveCurrentTenantIdentifier())
        .isEqualTo("tenant_schema");
    verify(statement).param("tenantId", tenantId);
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void failsClosedWhenTheAuthenticatedTenantSchemaCannotBeResolved() {
    UUID tenantId = UUID.randomUUID();
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> result = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
    when(statement.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(statement);
    when(statement.query(String.class)).thenReturn(result);
    when(result.single()).thenThrow(new IllegalStateException("database unavailable"));

    TenantContext.setCurrentTenant(tenantId);

    assertThatThrownBy(() -> new TenantIdentifierResolver(jdbc).resolveCurrentTenantIdentifier())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to resolve schema for tenant: " + tenantId);
  }
}
