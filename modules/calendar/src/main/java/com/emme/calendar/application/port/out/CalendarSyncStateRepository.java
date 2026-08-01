package com.emme.calendar.application.port.out;

import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import java.util.Optional;
import java.util.UUID;

/** Application-owned persistence port for Calendar synchronization state. */
public interface CalendarSyncStateRepository {

  Optional<CalendarSyncState> findByTenantIdAndProvider(UUID tenantId, CalendarProvider provider);

  CalendarSyncState save(CalendarSyncState state);
}
