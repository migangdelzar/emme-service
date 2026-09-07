package com.emme.tenancy.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.event.TenantRealmReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantActivationListenerTest {

  @Mock TenantProvisioningRepository provisioningRepository;
  @Mock ApplicationEventPublisher eventPublisher;
  @InjectMocks TenantActivationListener listener;

  @Test
  void onTenantRealmReady_activatesAndPublishes() {
    UUID tenantId = UUID.randomUUID();
    TenantRealmReady event = new TenantRealmReady(UUID.randomUUID(), tenantId, "slug", "emme-slug");
    when(provisioningRepository.claimActivation(tenantId)).thenReturn(true);
    when(provisioningRepository.findSchemaName(tenantId)).thenReturn("tenant_slug");

    listener.onTenantRealmReady(event);

    verify(provisioningRepository).claimActivation(tenantId);
    ArgumentCaptor<TenantActivated> captor = ArgumentCaptor.forClass(TenantActivated.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().keycloakRealm()).isEqualTo("emme-slug");
  }

  @Test
  void onTenantRealmReady_skipsWhenActivationClaimIsNotWon() {
    UUID tenantId = UUID.randomUUID();
    when(provisioningRepository.claimActivation(tenantId)).thenReturn(false);
    var listener = new TenantActivationListener(provisioningRepository, eventPublisher);

    listener.onTenantRealmReady(
        new TenantRealmReady(UUID.randomUUID(), tenantId, "slug", "emme-slug"));

    verify(provisioningRepository, never()).findSchemaName(tenantId);
    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void onTenantRealmReady_publishesOnlyForTheWinningDuplicateDelivery() {
    UUID tenantId = UUID.randomUUID();
    TenantRealmReady event = new TenantRealmReady(UUID.randomUUID(), tenantId, "slug", "emme-slug");
    when(provisioningRepository.claimActivation(tenantId)).thenReturn(true, false);
    when(provisioningRepository.findSchemaName(tenantId)).thenReturn("tenant_slug");

    listener.onTenantRealmReady(event);
    listener.onTenantRealmReady(event);

    verify(eventPublisher, times(1))
        .publishEvent(org.mockito.ArgumentMatchers.any(TenantActivated.class));
  }

  @Test
  void onTenantRealmReady_reconstructsTenantAndCorrelationContextForActivationWork() {
    UUID tenantId = UUID.randomUUID();
    TenantRealmReady event = new TenantRealmReady(UUID.randomUUID(), tenantId, "slug", "emme-slug");
    String correlationId = "tenant-realm-ready:" + event.eventId();
    when(provisioningRepository.claimActivation(tenantId)).thenReturn(true);
    when(provisioningRepository.findSchemaName(tenantId))
        .thenAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentTenantOptional()).contains(tenantId);
              assertThat(CorrelationContextHolder.requireCorrelationId()).isEqualTo(correlationId);
              return "tenant_slug";
            });
    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentTenantOptional()).contains(tenantId);
              assertThat(CorrelationContextHolder.requireCorrelationId()).isEqualTo(correlationId);
              return null;
            })
        .when(eventPublisher)
        .publishEvent(org.mockito.ArgumentMatchers.any(TenantActivated.class));

    listener.onTenantRealmReady(event);

    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
    assertThat(CorrelationId.get()).isNull();
  }
}
