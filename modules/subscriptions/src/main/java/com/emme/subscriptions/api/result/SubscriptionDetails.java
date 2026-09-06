package com.emme.subscriptions.api.result;

import com.emme.subscriptions.domain.model.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionDetails(
    UUID id,
    UUID tenantId,
    String plan,
    SubscriptionStatus status,
    Instant periodEndsAt,
    Instant createdAt) {}
