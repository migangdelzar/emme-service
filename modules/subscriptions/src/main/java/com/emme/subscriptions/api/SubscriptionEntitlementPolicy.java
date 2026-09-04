package com.emme.subscriptions.api;

import com.emme.subscriptions.api.type.PlanType;
import java.util.Set;

/** Public, pure capability policy used by modules that build authorization envelopes. */
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
}
