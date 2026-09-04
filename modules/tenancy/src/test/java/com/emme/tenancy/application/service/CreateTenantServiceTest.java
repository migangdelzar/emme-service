package com.emme.tenancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.application.port.out.TenantEventPublisher;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateTenantServiceTest {

  private final TenantRepository repository = org.mockito.Mockito.mock();
  private final TenantProvisioningRepository provisioningRepository = org.mockito.Mockito.mock();
  private final TenantEventPublisher eventPublisher = org.mockito.Mockito.mock();

  @Test
  void publishesTenantCreatedThroughTheProviderNeutralEventPort() {
    UUID tenantId = UUID.randomUUID();
    Tenant saved =
        Tenant.rehydrate(
            tenantId,
            "studio-a",
            "Studio A",
            TenantStatus.ACTIVE,
            null,
            "emme",
            Instant.parse("2026-09-04T00:00:00Z"),
            Instant.parse("2026-09-04T00:00:00Z"));
    when(repository.existsBySlug("studio-a")).thenReturn(false);
    when(repository.save(any(Tenant.class))).thenReturn(saved);

    TenantDetails result =
        new CreateTenantService(repository, provisioningRepository, eventPublisher)
            .create(new CreateTenantCommand("studio-a", "Studio A"));

    assertThat(result.id()).isEqualTo(tenantId);
    verify(provisioningRepository).requestProvisioning(tenantId, "studio-a", "studio_a");
    ArgumentCaptor<TenantCreated> event = ArgumentCaptor.forClass(TenantCreated.class);
    verify(eventPublisher).publish(event.capture());
    assertThat(event.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(event.getValue().slug()).isEqualTo("studio-a");
    assertThat(event.getValue().name()).isEqualTo("Studio A");
  }
}
