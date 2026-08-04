package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarSyncStateInfo;
import java.util.UUID;

/** Triggers synchronization of a tenant's external calendar events. */
public interface SyncCalendarEventsUseCase {

  CalendarSyncStateInfo sync(UUID tenantId);
}
