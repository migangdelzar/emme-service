package com.emme.notification.application.mapper;

import com.emme.notification.api.result.NotificationInfo;
import com.emme.notification.api.type.NotificationStatusView;
import com.emme.notification.domain.model.Notification;

public final class NotificationApplicationMapper {
  private NotificationApplicationMapper() {}

  public static NotificationInfo toInfo(Notification notification) {
    return new NotificationInfo(
        notification.id(),
        notification.tenantId(),
        notification.channel(),
        notification.recipientReference(),
        notification.body(),
        NotificationStatusView.valueOf(notification.status().name()),
        notification.createdAt());
  }
}
