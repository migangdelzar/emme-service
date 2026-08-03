package com.emme.notification.adapter.in.web.response;

import com.emme.notification.api.result.NotificationInfo;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID tenantId,
    String channel,
    String recipientReference,
    String status,
    Instant createdAt) {
  public static NotificationResponse from(NotificationInfo notification) {
    return new NotificationResponse(
        notification.id(),
        notification.tenantId(),
        notification.channel().name(),
        notification.recipientReference(),
        notification.status().name(),
        notification.createdAt());
  }
}
