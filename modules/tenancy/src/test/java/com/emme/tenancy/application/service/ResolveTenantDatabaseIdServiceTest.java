package com.emme.tenancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveTenantDatabaseIdServiceTest {

  @Test
  void resolvesTheDatabaseRoutingIdentityFromTheTenantRegistry() {
    TenantRepository repository = mock(TenantRepository.class);
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    when(repository.findDatabaseIdByTenantId(tenantId)).thenReturn(Optional.of(databaseId));

    UUID resolved = new ResolveTenantDatabaseIdService(repository).resolve(tenantId);

    assertThat(resolved).isEqualTo(databaseId);
  }
}
