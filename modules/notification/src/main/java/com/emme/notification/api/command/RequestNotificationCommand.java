package com.emme.notification.api.command;

import com.emme.kernel.type.NotificationChannel;
import java.util.UUID;

public record RequestNotificationCommand(
    UUID tenantId, NotificationChannel channel, String recipient, String message) {}
