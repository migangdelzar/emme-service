package com.emme.subscriptions.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.tenancy.api.event.TenantActivated;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionProvisioningListenerTest {

  @Mock private EnsureTenantSubscriptionUseCase provisioningService;

  @Test
  void provisionsInsideTheEventTenantContext() {
    UUID tenantId = UUID.randomUUID();
    var listener = new SubscriptionProvisioningListener(provisioningService);

    listener.onTenantActivated(
        new TenantActivated(null, tenantId, "studio", "studio_schema", "realm"));

    verify(provisioningService).ensure(tenantId);
  }

  @Test
  void propagatesOperationalProvisioningFailuresForRetry() {
    UUID tenantId = UUID.randomUUID();
    RuntimeException failure = new RuntimeException("database unavailable");
    doThrow(failure).when(provisioningService).ensure(tenantId);
    var listener = new SubscriptionProvisioningListener(provisioningService);

    assertThatThrownBy(
            () ->
                listener.onTenantActivated(
                    new TenantActivated(null, tenantId, "studio", "studio_schema", "realm")))
        .isSameAs(failure);
  }
}
