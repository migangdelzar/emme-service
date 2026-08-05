package com.emme.subscriptions.domain.exception;

import com.emme.subscriptions.api.type.PlanType;

public final class EntitlementViolationException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public EntitlementViolationException(PlanType plan, String entitlement) {
    super("Plan " + plan + " does not include entitlement: " + entitlement);
  }
}
