package com.emme.subscriptions.adapter.in.messaging.consumer;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.usecase.ResolveTenantDatabaseIdUseCase;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionProvisioningListener {

  private final EnsureTenantSubscriptionUseCase provisioning;
  private final ResolveTenantDatabaseIdUseCase databaseResolver;

  public SubscriptionProvisioningListener(
      EnsureTenantSubscriptionUseCase provisioning,
      ResolveTenantDatabaseIdUseCase databaseResolver) {
    this.provisioning = Objects.requireNonNull(provisioning, "provisioning must not be null");
    this.databaseResolver =
        Objects.requireNonNull(databaseResolver, "databaseResolver must not be null");
  }

  @ApplicationModuleListener(id = "subscriptions.tenant-activated.provisioning")
  public void onTenantActivated(TenantActivated event) {
    UUID databaseId = databaseResolver.resolve(event.tenantId());
    TenantContextHolder.withTenantAndCorrelation(
        event.tenantId(),
        databaseId,
        "tenant-activated:" + event.eventId(),
        () -> provisioning.ensure(event.tenantId()));
  }
}
