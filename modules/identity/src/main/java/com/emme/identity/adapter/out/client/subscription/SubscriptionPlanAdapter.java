package com.emme.identity.adapter.out.client.subscription;

import com.emme.identity.application.port.out.SubscriptionPlanPort;
import com.emme.subscriptions.api.query.GetSubscriptionPlanQuery;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.api.usecase.GetSubscriptionPlanUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapts the Studio subscription API to the Identity feature-flag port. */
@Component
@RequiredArgsConstructor
public final class SubscriptionPlanAdapter implements SubscriptionPlanPort {

  private final GetSubscriptionPlanUseCase getSubscriptionPlan;

  @Override
  public Optional<PlanType> findPlanForTenant(UUID tenantId) {
    return getSubscriptionPlan.getPlan(new GetSubscriptionPlanQuery(tenantId));
  }
}
