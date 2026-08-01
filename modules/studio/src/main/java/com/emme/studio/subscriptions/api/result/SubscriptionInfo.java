package com.emme.studio.subscriptions.api.result;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionInfo(
    UUID id, UUID tenantId, String plan, String status, Instant periodEndsAt, Instant createdAt) {}
