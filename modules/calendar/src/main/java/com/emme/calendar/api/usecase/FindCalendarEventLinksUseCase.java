package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import java.util.List;
import java.util.UUID;

/** Finds all external calendar links for an appointment. */
public interface FindCalendarEventLinksUseCase {

  List<CalendarEventLinkDetails> findByAppointmentId(UUID appointmentId);
}
