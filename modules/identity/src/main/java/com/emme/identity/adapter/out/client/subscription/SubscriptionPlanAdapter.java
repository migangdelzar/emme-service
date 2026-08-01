package com.emme.identity.adapter.out.client.subscription;

import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.studio.subscriptions.api.PlanType;
import com.emme.studio.subscriptions.application.SubscriptionService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts the Studio subscription API to the Identity feature-flag port. */
@Component
public final class SubscriptionPlanAdapter implements SubscriptionPlanPort {

  private final SubscriptionService subscriptionService;

  public SubscriptionPlanAdapter(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Override
  public Optional<PlanType> findPlanForTenant(UUID tenantId) {
    return subscriptionService.getPlanForTenant(tenantId);
  }
}
