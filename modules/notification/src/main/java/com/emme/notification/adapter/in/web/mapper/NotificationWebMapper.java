package com.emme.notification.adapter.in.web.mapper;

import com.emme.notification.adapter.in.web.request.RequestNotificationRequest;
import com.emme.notification.api.command.RequestNotificationCommand;
import java.util.UUID;

public final class NotificationWebMapper {
  private NotificationWebMapper() {}

  public static RequestNotificationCommand toCommand(
      UUID tenantId, RequestNotificationRequest request) {
    return new RequestNotificationCommand(
        tenantId, request.channel(), request.recipient(), request.message());
  }
}
