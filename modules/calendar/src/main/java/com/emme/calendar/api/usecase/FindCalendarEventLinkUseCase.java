package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.Optional;
import java.util.UUID;

/** Finds one external calendar link for an appointment in the active tenant schema. */
public interface FindCalendarEventLinkUseCase {

  Optional<CalendarEventLinkDetails> find(UUID appointmentId, CalendarProvider provider);
}
