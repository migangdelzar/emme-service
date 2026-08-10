package com.emme.calendar.application.mapper;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.domain.model.CalendarEventLink;

/** Maps calendar domain links to the public module result model. */
public final class CalendarEventLinkApplicationMapper {

  private CalendarEventLinkApplicationMapper() {}

  public static CalendarEventLinkDetails toResult(CalendarEventLink link) {
    return new CalendarEventLinkDetails(
        link.id(),
        link.appointmentId(),
        link.provider().name(),
        link.externalEventId(),
        link.etag(),
        link.status().name());
  }
}
