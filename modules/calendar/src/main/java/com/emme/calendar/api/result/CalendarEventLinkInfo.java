package com.emme.calendar.api.result;

import java.util.UUID;

/** Public DTO for CalendarEventLink — no entity details leaked. */
public record CalendarEventLinkInfo(
    UUID id,
    UUID appointmentId,
    String provider,
    String externalEventId,
    String etag,
    String status) {}
