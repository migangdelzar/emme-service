package com.emme.calendar.adapter.in.web.response;

import com.emme.calendar.api.result.CalendarSyncStateInfo;
import java.util.UUID;

/** HTTP representation of the Calendar synchronization state. */
public record CalendarSyncStateResponse(UUID id, UUID tenantId, String provider, String status) {

  public static CalendarSyncStateResponse from(CalendarSyncStateInfo state) {
    return new CalendarSyncStateResponse(
        state.id(), state.tenantId(), state.provider(), state.status());
  }
}
