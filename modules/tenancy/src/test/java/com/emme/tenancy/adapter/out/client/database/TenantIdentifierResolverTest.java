package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class TenantIdentifierResolverTest {

  @Test
  void resolvesTenantSchemaUsingTheBootstrapJdbcClient() {
    UUID tenantId = UUID.randomUUID();
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> result = mock(JdbcClient.MappedQuerySpec.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getBean("bootstrapJdbcClient", JdbcClient.class)).thenReturn(jdbc);
    when(jdbc.sql(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
    when(statement.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(statement);
    when(statement.query(String.class)).thenReturn(result);
    when(result.single()).thenReturn("tenant_schema");

    try (MockedStatic<ApplicationContextProvider> provider =
        mockStatic(ApplicationContextProvider.class)) {
      provider.when(ApplicationContextProvider::get).thenReturn(applicationContext);
      TenantContext.setCurrentTenant(tenantId);

      assertThat(new TenantIdentifierResolver().resolveCurrentTenantIdentifier())
          .isEqualTo("tenant_schema");
      verify(applicationContext).getBean("bootstrapJdbcClient", JdbcClient.class);
      verify(statement).param("tenantId", tenantId);
    }
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
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getBean("bootstrapJdbcClient", JdbcClient.class)).thenReturn(jdbc);
    when(jdbc.sql(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
    when(statement.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(statement);
    when(statement.query(String.class)).thenReturn(result);
    when(result.single()).thenThrow(new IllegalStateException("database unavailable"));

    try (MockedStatic<ApplicationContextProvider> provider =
        mockStatic(ApplicationContextProvider.class)) {
      provider.when(ApplicationContextProvider::get).thenReturn(applicationContext);
      TenantContext.setCurrentTenant(tenantId);

      assertThatThrownBy(() -> new TenantIdentifierResolver().resolveCurrentTenantIdentifier())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Unable to resolve schema for tenant: " + tenantId);
    }
  }
}
