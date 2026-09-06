package com.emme.calendar.adapter.in.web.response;

import com.emme.calendar.api.result.CalendarSyncStateDetails;
import com.emme.calendar.domain.model.CalendarSyncStatus;
import java.util.UUID;

/** HTTP representation of the Calendar synchronization state. */
public record CalendarSyncStateResponse(
    UUID id, UUID tenantId, String provider, CalendarSyncStatus status) {

  public static CalendarSyncStateResponse from(CalendarSyncStateDetails state) {
    return new CalendarSyncStateResponse(
        state.id(), state.tenantId(), state.provider(), state.status());
  }
}
