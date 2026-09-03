package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantIdentifierResolverTest {

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void failsClosedWhenTheAuthenticatedTenantSchemaCannotBeResolved() {
    UUID tenantId = UUID.randomUUID();
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    when(applicationContext.getBean("bootstrapJdbcTemplate", JdbcTemplate.class)).thenReturn(jdbc);
    when(jdbc.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(String.class), org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("database unavailable"));

    try (MockedStatic<ApplicationContextProvider> provider = mockStatic(ApplicationContextProvider.class)) {
      provider.when(ApplicationContextProvider::get).thenReturn(applicationContext);
      TenantContext.setCurrentTenant(tenantId);

      assertThatThrownBy(() -> new TenantIdentifierResolver().resolveCurrentTenantIdentifier())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Unable to resolve schema for tenant: " + tenantId);
    }
  }
}
