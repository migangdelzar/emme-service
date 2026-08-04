package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import java.util.UUID;

/** Marks a tenant-scoped calendar event link as synchronized. */
public interface MarkCalendarEventLinkSyncedUseCase {

  CalendarEventLinkDetails markSynced(UUID tenantId, UUID appointmentId, String etag);
}
