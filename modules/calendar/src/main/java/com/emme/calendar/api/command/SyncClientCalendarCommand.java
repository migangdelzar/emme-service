package com.emme.calendar.api.command;

import java.time.Instant;
import java.util.UUID;

/** Requests creation of a client appointment event in Google Calendar. */
public record SyncClientCalendarCommand(
    UUID tenantId,
    UUID appointmentId,
    String userId,
    Instant startsAt,
    Instant endsAt,
    String summary,
    String description) {}
