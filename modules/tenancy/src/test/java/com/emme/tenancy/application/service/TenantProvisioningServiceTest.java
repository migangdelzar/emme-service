package com.emme.tenancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.emme.tenancy.api.command.RequestTenantProvisioningCommand;
import com.emme.tenancy.api.query.GetTenantProvisioningStatusQuery;
import com.emme.tenancy.api.result.TenantProvisioningStatus;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

  @Mock private TenantProvisioningRepository provisioningRepository;

  @Test
  void delegatesProvisioningRequestCreationToTheRepositoryPort() {
    UUID tenantId = UUID.randomUUID();
    when(provisioningRepository.requestProvisioning(org.mockito.ArgumentMatchers.any(), eq("studio-a"), eq("studio_a"))).thenReturn(tenantId);
    RequestTenantProvisioningService service =
        new RequestTenantProvisioningService(provisioningRepository);

    UUID result =
        service.request(
            new RequestTenantProvisioningCommand(
                "studio-a", "Studio A", "America/Mexico_City", "en"));

    assertThat(result).isEqualTo(tenantId);
    verify(provisioningRepository).requestProvisioning(org.mockito.ArgumentMatchers.any(), eq("studio-a"), eq("studio_a"));
  }

  @Test
  void mapsRepositoryStatusToThePublicProvisioningStatus() {
    UUID tenantId = UUID.randomUUID();
    Instant migratedAt = Instant.parse("2026-08-01T00:00:00Z");
    when(provisioningRepository.findStatus(tenantId))
        .thenReturn(
            new TenantProvisioningRepository.TenantProvisioningStatus(
                "ACTIVE", "studio_a", migratedAt, null));
    GetTenantProvisioningStatusService service =
        new GetTenantProvisioningStatusService(provisioningRepository);

    TenantProvisioningStatus result = service.get(new GetTenantProvisioningStatusQuery(tenantId));

    assertThat(result.status()).isEqualTo("ACTIVE");
    assertThat(result.schemaName()).isEqualTo("studio_a");
    assertThat(result.lastMigratedAt()).isEqualTo(migratedAt);
    assertThat(result.error()).isNull();
  }
}
