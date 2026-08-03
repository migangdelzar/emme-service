package com.emme.calendar.adapter.in.web.response;

import com.emme.calendar.api.result.ClientCalendarSyncDetails;

/** HTTP response for a client-calendar synchronization. */
public record ClientCalendarSyncResponse(String status, String eventId) {

  public static ClientCalendarSyncResponse from(ClientCalendarSyncDetails details) {
    return new ClientCalendarSyncResponse(details.status(), details.eventId());
  }
}
