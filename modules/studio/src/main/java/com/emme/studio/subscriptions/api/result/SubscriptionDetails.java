package com.emme.studio.subscriptions.api.result;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionDetails(
    UUID id, UUID tenantId, String plan, String status, Instant periodEndsAt, Instant createdAt) {}
