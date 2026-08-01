package com.emme.calendar.application.mapper;

import com.emme.calendar.api.result.CalendarEventLinkInfo;
import com.emme.calendar.domain.model.CalendarEventLink;

/** Maps calendar domain links to the public module result model. */
public final class CalendarEventLinkApplicationMapper {

  private CalendarEventLinkApplicationMapper() {}

  public static CalendarEventLinkInfo toInfo(CalendarEventLink link) {
    return new CalendarEventLinkInfo(
        link.id(),
        link.appointmentId(),
        link.provider().name(),
        link.externalEventId(),
        link.etag(),
        link.status().name());
  }
}
