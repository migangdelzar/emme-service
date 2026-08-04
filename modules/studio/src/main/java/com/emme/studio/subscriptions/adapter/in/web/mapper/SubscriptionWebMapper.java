package com.emme.studio.subscriptions.adapter.in.web.mapper;

import com.emme.studio.subscriptions.adapter.in.web.response.SubscriptionResponse;
import com.emme.studio.subscriptions.api.result.SubscriptionDetails;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionWebMapper {
  public SubscriptionResponse toResponse(SubscriptionDetails subscription) {
    return new SubscriptionResponse(
        subscription.id(),
        subscription.tenantId(),
        subscription.plan(),
        subscription.status(),
        subscription.periodEndsAt(),
        subscription.createdAt());
  }
}
