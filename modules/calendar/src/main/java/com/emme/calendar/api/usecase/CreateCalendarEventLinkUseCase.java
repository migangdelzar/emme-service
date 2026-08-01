package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkInfo;
import java.util.UUID;

/** Creates a pending external calendar event link. */
public interface CreateCalendarEventLinkUseCase {

  CalendarEventLinkInfo create(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId);
}
