package com.emme.studio.subscriptions.api;

import java.util.UUID;

/**
 * Public API for subscription operations. Business modules depend on this interface, not on
 * internal entity classes.
 */
public interface SubscriptionApi {
  PlanInfo getCurrentPlan(UUID tenantId);

  boolean checkEntitlement(UUID tenantId, String feature);
}
