package com.emme.subscriptions.api.result;

import com.emme.subscriptions.api.type.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionDetails(
    UUID id,
    UUID tenantId,
    String plan,
    SubscriptionStatus status,
    Instant periodEndsAt,
    Instant createdAt) {}
