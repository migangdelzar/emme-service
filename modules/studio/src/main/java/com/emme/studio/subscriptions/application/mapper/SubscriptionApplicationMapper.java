package com.emme.studio.subscriptions.application.mapper;

import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import com.emme.studio.subscriptions.domain.model.Subscription;

public final class SubscriptionApplicationMapper {
  private SubscriptionApplicationMapper() {}

  public static SubscriptionInfo toInfo(Subscription subscription) {
    return new SubscriptionInfo(
        subscription.id(),
        subscription.tenantId(),
        subscription.plan().name(),
        subscription.status().name(),
        subscription.periodEndsAt(),
        subscription.createdAt());
  }
}
