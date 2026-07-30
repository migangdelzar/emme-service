package com.emme.calendar.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the calendar module when an appointment change triggers calendar sync. Consumed by
 * the google module to execute actual Google Calendar API calls.
 */
public record CalendarSyncRequested(
    UUID tenantId,
    UUID appointmentId,
    String action, // CREATE, UPDATE, DELETE
    String summary,
    String description,
    Instant startsAt,
    Instant endsAt,
    String oldExternalEventId // null for CREATE, the existing event ID for UPDATE/DELETE
    ) {}
