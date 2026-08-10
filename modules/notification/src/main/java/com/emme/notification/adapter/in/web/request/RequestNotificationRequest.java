package com.emme.notification.adapter.in.web.request;

import com.emme.kernel.type.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestNotificationRequest(
    @NotNull NotificationChannel channel, @NotBlank String recipient, @NotBlank String message) {}
