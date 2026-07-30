package com.emme.studio.subscriptions.application;

import com.emme.studio.subscriptions.api.PlanType;

public class EntitlementViolationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EntitlementViolationException(PlanType plan, String entitlement) {
    super("Plan " + plan + " does not include entitlement: " + entitlement);
  }
}
