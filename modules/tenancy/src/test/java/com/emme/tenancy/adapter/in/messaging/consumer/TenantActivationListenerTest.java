package com.emme.tenancy.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    when(provisioningRepository.findSchemaName(tenantId)).thenReturn("tenant_slug");

    listener.onTenantRealmReady(event);

    verify(provisioningRepository).markActive(tenantId);
    ArgumentCaptor<TenantActivated> captor = ArgumentCaptor.forClass(TenantActivated.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().keycloakRealm()).isEqualTo("emme-slug");
  }
}
