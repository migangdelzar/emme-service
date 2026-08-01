package com.emme.notification.api.command;

import java.util.UUID;

public record CancelNotificationCommand(UUID notificationId) {}
