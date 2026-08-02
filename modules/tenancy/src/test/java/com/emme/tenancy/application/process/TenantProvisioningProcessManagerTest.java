package com.emme.tenancy.application.process;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningProcessManagerTest {

  @Mock private TenantProvisioningRepository provisioningRepository;

  @Mock private TenantSchemaMigrationPort schemaMigrationPort;

  @Test
  void doesNotInvokeMigrationWhenThereAreNoPendingRequests() {
    when(provisioningRepository.findPending()).thenReturn(List.of());

    new TenantProvisioningProcessManager(provisioningRepository, schemaMigrationPort)
        .processProvisioningRequests();

    verify(provisioningRepository).findPending();
    verifyNoInteractions(schemaMigrationPort);
  }

  @Test
  void toleratesProvisioningRepositoryFailureForTheNextScheduledRetry() {
    when(provisioningRepository.findPending())
        .thenThrow(new IllegalStateException("database unavailable"));

    new TenantProvisioningProcessManager(provisioningRepository, schemaMigrationPort)
        .processProvisioningRequests();

    verify(provisioningRepository).findPending();
    verifyNoInteractions(schemaMigrationPort);
  }

  @Test
  void marksTheTenantActiveAfterSchemaMigration() {
    TenantProvisioningRepository.TenantProvisioningRequest request = request();
    when(provisioningRepository.findPending()).thenReturn(List.of(request));

    new TenantProvisioningProcessManager(provisioningRepository, schemaMigrationPort)
        .processProvisioningRequests();

    verify(schemaMigrationPort).migrate("studio_a");
    verify(provisioningRepository).markActive(request.tenantId());
  }

  @Test
  void recordsABoundedFailureWhenSchemaMigrationFails() {
    TenantProvisioningRepository.TenantProvisioningRequest request = request();
    when(provisioningRepository.findPending()).thenReturn(List.of(request));
    doThrow(new IllegalStateException("migration failed"))
        .when(schemaMigrationPort)
        .migrate("studio_a");

    new TenantProvisioningProcessManager(provisioningRepository, schemaMigrationPort)
        .processProvisioningRequests();

    verify(provisioningRepository).markFailed(request.tenantId(), "migration failed");
  }

  private TenantProvisioningRepository.TenantProvisioningRequest request() {
    return new TenantProvisioningRepository.TenantProvisioningRequest(
        UUID.randomUUID(), "studio-a", "studio_a");
  }
}
