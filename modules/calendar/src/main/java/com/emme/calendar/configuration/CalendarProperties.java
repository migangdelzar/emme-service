package com.emme.calendar.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Calendar settings bound to {@code app.calendar.*}. */
@ConfigurationProperties(prefix = "app.calendar")
public record CalendarProperties(String calendarId) {

  public CalendarProperties {
    calendarId = calendarId == null ? "primary" : calendarId;
  }
}
