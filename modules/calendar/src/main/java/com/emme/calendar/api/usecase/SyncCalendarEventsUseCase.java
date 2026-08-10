package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarSyncStateDetails;
import java.util.UUID;

/** Triggers synchronization of a tenant's external calendar events. */
public interface SyncCalendarEventsUseCase {

  CalendarSyncStateDetails sync(UUID tenantId);
}
