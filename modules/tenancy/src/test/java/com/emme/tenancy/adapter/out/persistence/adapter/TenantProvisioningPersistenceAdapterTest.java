package com.emme.tenancy.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.tenancy.adapter.out.persistence.entity.TenantRegistryEntity;
import com.emme.tenancy.adapter.out.persistence.repository.SpringDataTenantRegistryRepository;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantProvisioningPersistenceAdapterTest {

  @Test
  void requestProvisioning_insertsNewRegistryEntry() {
    var repository = mock(SpringDataTenantRegistryRepository.class);
    UUID tenantId = UUID.randomUUID();
    when(repository.findBySlug("studio-a")).thenReturn(Optional.empty());

    UUID result =
        new TenantProvisioningPersistenceAdapter(repository)
            .requestProvisioning(tenantId, "studio-a", "studio_a");

    assertThat(result).isEqualTo(tenantId);
    verify(repository).save(org.mockito.ArgumentMatchers.any(TenantRegistryEntity.class));
  }

  @Test
  void requestProvisioning_skipsExistingSlug() {
    var repository = mock(SpringDataTenantRegistryRepository.class);
    UUID tenantId = UUID.randomUUID();
    UUID existingTenantId = UUID.randomUUID();
    when(repository.findBySlug("studio-a"))
        .thenReturn(
            Optional.of(
                new TenantRegistryEntity(
                    existingTenantId,
                    "studio-a",
                    "studio_a",
                    TenantProvisioningState.PROVISIONING)));

    UUID result =
        new TenantProvisioningPersistenceAdapter(repository)
            .requestProvisioning(tenantId, "studio-a", "studio_a");

    assertThat(result).isEqualTo(existingTenantId);
    verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void findSchemaName_returnsSchemaName() {
    var repository = mock(SpringDataTenantRegistryRepository.class);
    UUID tenantId = UUID.randomUUID();
    when(repository.findByTenantId(tenantId))
        .thenReturn(
            Optional.of(
                new TenantRegistryEntity(
                    tenantId, "slug", "schema_name", TenantProvisioningState.ACTIVE)));

    String result = new TenantProvisioningPersistenceAdapter(repository).findSchemaName(tenantId);
    assertThat(result).isEqualTo("schema_name");
  }
}
