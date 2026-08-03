package com.emme.calendar.adapter.in.web.response;

import com.emme.calendar.domain.model.CalendarSyncState;
import java.util.UUID;

/** HTTP representation of the Calendar synchronization state. */
public record CalendarSyncStateResponse(UUID id, UUID tenantId, String provider, String status) {

  public static CalendarSyncStateResponse from(CalendarSyncState state) {
    return new CalendarSyncStateResponse(
        state.id(), state.tenantId(), state.provider().name(), state.status().name());
  }
}
