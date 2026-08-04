package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import java.util.Optional;
import java.util.UUID;

/** Finds one tenant-scoped external calendar link for an appointment. */
public interface FindCalendarEventLinkUseCase {

  Optional<CalendarEventLinkDetails> find(UUID tenantId, UUID appointmentId);
}
