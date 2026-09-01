package com.emme.tenancy.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAiContextResolverTest {

  @Test
  void resolvesTheTenantDatabaseIntoAnAuthoritativeExecutionContext() {
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    TenantRepository tenants = mock(TenantRepository.class);
    when(tenants.findDatabaseIdByTenantId(tenantId)).thenReturn(Optional.of(databaseId));
    TenantAiContextResolver resolver =
        new TenantAiContextResolver(tenants, TenantPoolingProperties.defaults());

    var context = resolver.resolve(tenantId, "correlation");

    assertThat(context.tenantId()).isEqualTo(tenantId);
    assertThat(context.databaseId()).isEqualTo(databaseId);
    assertThat(context.correlationId()).isEqualTo("correlation");
  }

  @Test
  void failsClosedWhenNeitherTheTenantNorTheDefaultDatabaseCanBeResolved() {
    UUID tenantId = UUID.randomUUID();
    TenantRepository tenants = mock(TenantRepository.class);
    when(tenants.findDatabaseIdByTenantId(tenantId)).thenReturn(Optional.empty());
    TenantAiContextResolver resolver =
        new TenantAiContextResolver(
            tenants, new TenantPoolingProperties(200, 30, 60, 5, 20, 100, "not-a-uuid"));

    assertThatThrownBy(() -> resolver.resolve(tenantId, "correlation"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No valid default database for semantic cache invalidation");
  }
}
