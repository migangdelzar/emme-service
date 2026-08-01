package com.emme.calendar.api.event;

import java.time.Instant;
import java.util.UUID;

/** Public fact raised when an appointment change requires calendar synchronization. */
public record CalendarSyncRequested(
    UUID tenantId,
    UUID appointmentId,
    String action,
    String summary,
    String description,
    Instant startsAt,
    Instant endsAt,
    String oldExternalEventId) {}
