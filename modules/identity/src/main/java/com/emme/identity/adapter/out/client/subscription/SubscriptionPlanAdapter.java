package com.emme.identity.adapter.out.client.subscription;

import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.studio.subscriptions.api.query.GetSubscriptionPlanQuery;
import com.emme.studio.subscriptions.api.type.PlanType;
import com.emme.studio.subscriptions.api.usecase.GetSubscriptionPlanUseCase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts the Studio subscription API to the Identity feature-flag port. */
@Component
public final class SubscriptionPlanAdapter implements SubscriptionPlanPort {

  private final GetSubscriptionPlanUseCase getSubscriptionPlan;

  public SubscriptionPlanAdapter(GetSubscriptionPlanUseCase getSubscriptionPlan) {
    this.getSubscriptionPlan = getSubscriptionPlan;
  }

  @Override
  public Optional<PlanType> findPlanForTenant(UUID tenantId) {
    return getSubscriptionPlan.getPlan(new GetSubscriptionPlanQuery(tenantId));
  }
}
