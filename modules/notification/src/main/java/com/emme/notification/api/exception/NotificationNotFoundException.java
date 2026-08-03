package com.emme.notification.api.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotificationNotFoundException(UUID notificationId) {
    super("Notification not found: " + notificationId);
  }
}
