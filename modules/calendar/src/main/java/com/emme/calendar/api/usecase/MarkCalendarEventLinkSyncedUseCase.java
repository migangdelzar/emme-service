package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import java.util.UUID;

/** Marks a calendar event link in the active tenant schema as synchronized. */
public interface MarkCalendarEventLinkSyncedUseCase {

  CalendarEventLinkDetails markSynced(UUID appointmentId, String provider, String etag);
}
