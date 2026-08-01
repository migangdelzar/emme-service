package com.emme.identity.application.port.out;

import com.emme.studio.subscriptions.api.PlanType;
import java.util.Optional;
import java.util.UUID;

/** Subscription capability required to evaluate plan-gated feature flags. */
@FunctionalInterface
public interface SubscriptionPlanPort {

  Optional<PlanType> findPlanForTenant(UUID tenantId);
}
