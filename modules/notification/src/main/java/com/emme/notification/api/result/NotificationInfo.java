package com.emme.notification.api.result;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.api.type.NotificationStatusView;
import java.time.Instant;
import java.util.UUID;

public record NotificationInfo(
    UUID id,
    UUID tenantId,
    NotificationChannel channel,
    String recipientReference,
    String body,
    NotificationStatusView status,
    Instant createdAt) {}
