package com.emme.calendar.api.type;

/** Stable calendar event-link lifecycle values exposed by the application API. */
public enum CalendarEventLinkStatus {
  PENDING,
  SYNCED,
  CONFLICT,
  DELETED,
  FAILED
}
