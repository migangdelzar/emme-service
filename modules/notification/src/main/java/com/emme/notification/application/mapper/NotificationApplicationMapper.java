package com.emme.notification.application.mapper;

import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.type.NotificationStatus;
import com.emme.notification.domain.model.Notification;

public final class NotificationApplicationMapper {
  private NotificationApplicationMapper() {}

  public static NotificationDetails toResult(Notification notification) {
    return new NotificationDetails(
        notification.id(),
        notification.tenantId(),
        notification.channel(),
        notification.recipientReference(),
        notification.body(),
        NotificationStatus.valueOf(notification.status().name()),
        notification.createdAt());
  }
}
