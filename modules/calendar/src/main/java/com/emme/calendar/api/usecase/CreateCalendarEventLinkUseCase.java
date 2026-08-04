package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import java.util.UUID;

/** Creates a pending external calendar event link. */
public interface CreateCalendarEventLinkUseCase {

  CalendarEventLinkDetails create(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId);
}
