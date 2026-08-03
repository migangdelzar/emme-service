package com.emme.calendar.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Google Calendar service-account and endpoint settings. */
@ConfigurationProperties(prefix = "app.calendar.google")
public record GoogleCalendarProperties(
    String serviceAccountJsonBase64, String tokenUrl, String freeBusyUrl) {

  public GoogleCalendarProperties {
    serviceAccountJsonBase64 = serviceAccountJsonBase64 == null ? "" : serviceAccountJsonBase64;
    tokenUrl = tokenUrl == null ? "https://oauth2.googleapis.com/token" : tokenUrl;
    freeBusyUrl =
        freeBusyUrl == null ? "https://www.googleapis.com/calendar/v3/freeBusy" : freeBusyUrl;
  }
}
