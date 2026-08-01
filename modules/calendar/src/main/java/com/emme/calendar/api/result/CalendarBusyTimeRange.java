package com.emme.calendar.api.result;

import java.time.LocalTime;

/** Public read result representing a busy period in a calendar. */
public record CalendarBusyTimeRange(LocalTime start, LocalTime end) {}
