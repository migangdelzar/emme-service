package com.emme.calendar.application.port.out;

import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Application-owned persistence port for Calendar event links. */
public interface CalendarEventLinkRepository {

  List<CalendarEventLink> findByAppointmentId(UUID appointmentId);

  Optional<CalendarEventLink> findByAppointmentIdAndProvider(
      UUID appointmentId, CalendarProvider provider);

  CalendarEventLink save(CalendarEventLink link);

  List<CalendarEventLink> saveAll(List<CalendarEventLink> links);
}
