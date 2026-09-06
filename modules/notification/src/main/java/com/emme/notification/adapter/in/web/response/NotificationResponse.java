package com.emme.notification.adapter.in.web.response;

import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.type.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID tenantId,
    String channel,
    String recipientReference,
    NotificationStatus status,
    Instant createdAt) {
  public static NotificationResponse from(NotificationDetails notification) {
    return new NotificationResponse(
        notification.id(),
        notification.tenantId(),
        notification.channel().name(),
        notification.recipientReference(),
        notification.status(),
        notification.createdAt());
  }
}
