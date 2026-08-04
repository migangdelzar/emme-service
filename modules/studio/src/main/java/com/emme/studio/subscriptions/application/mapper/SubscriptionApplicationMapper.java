package com.emme.studio.subscriptions.application.mapper;

import com.emme.studio.subscriptions.api.result.SubscriptionDetails;
import com.emme.studio.subscriptions.domain.model.Subscription;

public final class SubscriptionApplicationMapper {
  private SubscriptionApplicationMapper() {}

  public static SubscriptionDetails toResult(Subscription subscription) {
    return new SubscriptionDetails(
        subscription.id(),
        subscription.tenantId(),
        subscription.plan().name(),
        subscription.status().name(),
        subscription.periodEndsAt(),
        subscription.createdAt());
  }
}
