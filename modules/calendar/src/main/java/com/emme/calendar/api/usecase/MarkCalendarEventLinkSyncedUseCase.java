package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.UUID;

/** Marks a calendar event link in the active tenant schema as synchronized. */
public interface MarkCalendarEventLinkSyncedUseCase {

  CalendarEventLinkDetails markSynced(UUID appointmentId, CalendarProvider provider, String etag);
}
