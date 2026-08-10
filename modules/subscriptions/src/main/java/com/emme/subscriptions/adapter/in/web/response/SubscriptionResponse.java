package com.emme.subscriptions.adapter.in.web.response;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id, UUID tenantId, String plan, String status, Instant periodEndsAt, Instant createdAt) {}
