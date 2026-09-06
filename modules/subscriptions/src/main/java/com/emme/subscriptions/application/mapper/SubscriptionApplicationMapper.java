package com.emme.subscriptions.application.mapper;

import com.emme.subscriptions.api.result.SubscriptionDetails;
import com.emme.subscriptions.api.type.SubscriptionStatus;
import com.emme.subscriptions.domain.model.Subscription;

public final class SubscriptionApplicationMapper {
  private SubscriptionApplicationMapper() {}

  public static SubscriptionDetails toResult(Subscription subscription) {
    return new SubscriptionDetails(
        subscription.id(),
        subscription.tenantId(),
        subscription.plan().name(),
        SubscriptionStatus.valueOf(subscription.status().name()),
        subscription.periodEndsAt(),
        subscription.createdAt());
  }
}
