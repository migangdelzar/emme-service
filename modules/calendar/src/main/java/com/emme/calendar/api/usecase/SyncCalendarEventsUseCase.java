package com.emme.calendar.api.usecase;

import com.emme.calendar.domain.model.CalendarSyncState;
import java.util.UUID;

/** Triggers synchronization of a tenant's external calendar events. */
public interface SyncCalendarEventsUseCase {

  CalendarSyncState sync(UUID tenantId);
}
