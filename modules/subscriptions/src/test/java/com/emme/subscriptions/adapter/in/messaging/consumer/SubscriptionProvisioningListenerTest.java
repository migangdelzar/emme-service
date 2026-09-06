package com.emme.subscriptions.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationContextHolder;
import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.usecase.ResolveTenantDatabaseIdUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionProvisioningListenerTest {

  @Mock private EnsureTenantSubscriptionUseCase provisioningService;
  @Mock private ResolveTenantDatabaseIdUseCase databaseResolver;

  @Test
  void restoresTenantDatabaseAndCorrelationBeforeProvisioning() {
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    TenantActivated event =
        new TenantActivated(UUID.randomUUID(), tenantId, "studio", "studio_schema", "realm");
    doAnswer(
            ignored -> {
              assertThat(TenantContextHolder.currentTenantOptional()).contains(tenantId);
              assertThat(TenantContextHolder.currentDatabaseOptional()).contains(databaseId);
              assertThat(CorrelationContextHolder.requireCorrelationId())
                  .isEqualTo("tenant-activated:" + event.eventId());
              return null;
            })
        .when(provisioningService)
        .ensure(tenantId);
    var listener = new SubscriptionProvisioningListener(provisioningService, databaseResolver);
    org.mockito.Mockito.when(databaseResolver.resolve(tenantId)).thenReturn(databaseId);

    listener.onTenantActivated(event);

    verify(databaseResolver).resolve(tenantId);
    verify(provisioningService).ensure(tenantId);
    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
    assertThat(TenantContextHolder.currentDatabaseOptional()).isEmpty();
  }

  @Test
  void provisionsInsideTheEventTenantContext() {
    UUID tenantId = UUID.randomUUID();
    var listener = new SubscriptionProvisioningListener(provisioningService, databaseResolver);

    listener.onTenantActivated(
        new TenantActivated(null, tenantId, "studio", "studio_schema", "realm"));

    verify(provisioningService).ensure(tenantId);
  }

  @Test
  void propagatesOperationalProvisioningFailuresForRetry() {
    UUID tenantId = UUID.randomUUID();
    RuntimeException failure = new RuntimeException("database unavailable");
    doThrow(failure).when(provisioningService).ensure(tenantId);
    var listener = new SubscriptionProvisioningListener(provisioningService, databaseResolver);

    assertThatThrownBy(
            () ->
                listener.onTenantActivated(
                    new TenantActivated(null, tenantId, "studio", "studio_schema", "realm")))
        .isSameAs(failure);
  }
}
