package com.emme.calendar.api.result;

import com.emme.calendar.api.type.CalendarEventLinkStatus;
import java.util.UUID;

/** Public DTO for CalendarEventLink — no entity details leaked. */
public record CalendarEventLinkDetails(
    UUID id,
    UUID appointmentId,
    String provider,
    String externalEventId,
    String etag,
    CalendarEventLinkStatus status) {}
