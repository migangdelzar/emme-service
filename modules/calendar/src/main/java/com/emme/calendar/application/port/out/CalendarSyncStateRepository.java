package com.emme.calendar.application.port.out;

import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import java.util.Optional;

/** Application-owned persistence port for Calendar synchronization state. */
public interface CalendarSyncStateRepository {

  Optional<CalendarSyncState> findByProvider(CalendarProvider provider);

  CalendarSyncState save(CalendarSyncState state);
}
