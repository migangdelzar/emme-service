package com.emme.studio.subscriptions.application;

import com.emme.studio.subscriptions.api.PlanType;
import java.util.Set;

public final class EntitlementEnforcer {

  private EntitlementEnforcer() {}

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
