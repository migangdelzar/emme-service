package com.emme.tenancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningApplicationServiceTest {

  @Mock private TenantProvisioningRepository provisioningRepository;

  @Test
  void delegatesProvisioningRequestCreationToTheRepositoryPort() {
    UUID tenantId = UUID.randomUUID();
    when(provisioningRepository.requestProvisioning("studio-a", "studio_a")).thenReturn(tenantId);
    TenantProvisioningApplicationService service =
        new TenantProvisioningApplicationService(provisioningRepository);

    UUID result = service.requestProvisioning("studio-a", "Studio A", "America/Mexico_City", "en");

    assertThat(result).isEqualTo(tenantId);
    verify(provisioningRepository).requestProvisioning("studio-a", "studio_a");
  }

  @Test
  void mapsRepositoryStatusToThePublicProvisioningStatus() {
    UUID tenantId = UUID.randomUUID();
    Instant migratedAt = Instant.parse("2026-08-01T00:00:00Z");
    when(provisioningRepository.findStatus(tenantId))
        .thenReturn(
            new TenantProvisioningRepository.TenantProvisioningStatus(
                "ACTIVE", "studio_a", migratedAt, null));
    TenantProvisioningApplicationService service =
        new TenantProvisioningApplicationService(provisioningRepository);

    TenantProvisioningService.TenantStatus result = service.getStatus(tenantId);

    assertThat(result.status()).isEqualTo("ACTIVE");
    assertThat(result.schemaName()).isEqualTo("studio_a");
    assertThat(result.lastMigratedAt()).isEqualTo(migratedAt);
    assertThat(result.error()).isNull();
  }
}
