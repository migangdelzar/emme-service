package com.emme.studio.subscriptions.domain.service;

import com.emme.studio.subscriptions.api.type.PlanType;
import com.emme.studio.subscriptions.domain.exception.EntitlementViolationException;
import java.util.Set;

/** Pure subscription policy for plan-gated capabilities. */
public final class SubscriptionEntitlementPolicy {
  private SubscriptionEntitlementPolicy() {}

  public static Set<String> getEntitlements(PlanType plan) {
    return switch (plan) {
      case STARTER -> Set.of("customers:read", "services:read", "appointments:read");
      case PRO ->
          Set.of(
              "customers:read",
              "customers:write",
              "services:read",
              "services:write",
              "appointments:read",
              "appointments:write",
              "ai:basic");
      case ENTERPRISE ->
          Set.of(
              "customers:read",
              "customers:write",
              "services:read",
              "services:write",
              "appointments:read",
              "appointments:write",
              "ai:basic",
              "ai:advanced",
              "analytics:export",
              "calendar:sync");
    };
  }

  public static void enforce(PlanType plan, String entitlement) {
    if (!getEntitlements(plan).contains(entitlement)) {
      throw new EntitlementViolationException(plan, entitlement);
    }
  }
}
