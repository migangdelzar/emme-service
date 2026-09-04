package com.emme.subscriptions.adapter.in.messaging.consumer;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.subscriptions.api.usecase.EnsureTenantSubscriptionUseCase;
import com.emme.tenancy.api.event.TenantActivated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionProvisioningListener {

  private final EnsureTenantSubscriptionUseCase provisioning;

  public SubscriptionProvisioningListener(EnsureTenantSubscriptionUseCase provisioning) {
    this.provisioning = provisioning;
  }

  @ApplicationModuleListener
  public void onTenantActivated(TenantActivated event) {
    TenantContextHolder.withTenantOverride(
        event.tenantId(), () -> provisioning.ensure(event.tenantId()));
  }
}
