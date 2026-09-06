package com.emme.subscriptions.adapter.in.web.response;

import com.emme.subscriptions.api.type.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID tenantId,
    String plan,
    SubscriptionStatus status,
    Instant periodEndsAt,
    Instant createdAt) {}
