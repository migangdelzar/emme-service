package com.emme.subscriptions.api.type;

/** Stable subscription lifecycle values exposed by the application API. */
public enum SubscriptionStatus {
  TRIAL,
  ACTIVE,
  PAST_DUE,
  SUSPENDED,
  CANCELLED
}
