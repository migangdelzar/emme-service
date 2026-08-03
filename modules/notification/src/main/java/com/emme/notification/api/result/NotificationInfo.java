package com.emme.notification.api.result;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationInfo(
    UUID id,
    UUID tenantId,
    NotificationChannel channel,
    String recipientReference,
    String body,
    NotificationStatus status,
    Instant createdAt) {}
