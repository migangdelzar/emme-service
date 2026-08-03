package com.emme.studio.subscriptions.domain.exception;

import com.emme.studio.subscriptions.api.type.PlanType;

public final class EntitlementViolationException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public EntitlementViolationException(PlanType plan, String entitlement) {
    super("Plan " + plan + " does not include entitlement: " + entitlement);
  }
}
