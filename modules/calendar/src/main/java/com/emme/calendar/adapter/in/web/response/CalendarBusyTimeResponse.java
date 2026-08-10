package com.emme.calendar.adapter.in.web.response;

import com.emme.calendar.api.result.CalendarBusyTimeRange;

/** HTTP representation of one busy interval. */
public record CalendarBusyTimeResponse(String start, String end) {

  public static CalendarBusyTimeResponse from(CalendarBusyTimeRange range) {
    return new CalendarBusyTimeResponse(range.start().toString(), range.end().toString());
  }
}
