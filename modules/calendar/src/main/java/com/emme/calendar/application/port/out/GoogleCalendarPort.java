package com.emme.calendar.application.port.out;

import com.emme.calendar.api.result.CalendarBusyTimeRange;
import java.util.List;

/** Application-owned port for querying external calendar availability. */
public interface GoogleCalendarPort {

  boolean isConfigured();

  List<CalendarBusyTimeRange> freeBusy(String calendarId, String timeMin, String timeMax);
}
