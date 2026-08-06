package com.emme.tenancy.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.event.TenantSchemaReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantSchemaProvisioningListenerTest {

  @Mock TenantSchemaMigrationPort schemaMigrationPort;
  @Mock TenantProvisioningRepository provisioningRepository;
  @Mock ApplicationEventPublisher eventPublisher;
  @InjectMocks TenantSchemaProvisioningListener listener;

  @Test
  void onTenantCreated_publishesSchemaReady() {
    UUID tenantId = UUID.randomUUID();
    TenantCreated event =
        new TenantCreated(
            UUID.randomUUID(), tenantId, "test-studio", "Test Studio");
    when(schemaMigrationPort.migrate(tenantId, "test-studio")).thenReturn("test_studio");

    listener.onTenantCreated(event);

    ArgumentCaptor<TenantSchemaReady> captor = ArgumentCaptor.forClass(TenantSchemaReady.class);
    verify(eventPublisher).publishEvent(captor.capture());
    TenantSchemaReady ready = captor.getValue();
    assertThat(ready.tenantId()).isEqualTo(tenantId);
    assertThat(ready.slug()).isEqualTo("test-studio");
    assertThat(ready.schemaName()).isEqualTo("test_studio");
  }

  @Test
  void onTenantCreated_marksFailedAndRethrows_onException() {
    UUID tenantId = UUID.randomUUID();
    TenantCreated event =
        new TenantCreated(UUID.randomUUID(), tenantId, "test", "Test");
    RuntimeException ex = new RuntimeException("DB down");
    when(schemaMigrationPort.migrate(tenantId, "test")).thenThrow(ex);

    try {
      listener.onTenantCreated(event);
    } catch (RuntimeException caught) {
      assertThat(caught).isSameAs(ex);
    }
    verify(provisioningRepository).markFailed(tenantId, ex.getMessage());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
