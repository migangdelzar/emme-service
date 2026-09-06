package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarSyncStateDetails;
import com.emme.calendar.api.usecase.SyncCalendarEventsUseCase;
import com.emme.calendar.application.port.out.CalendarSyncStateRepository;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for tenant calendar synchronization. */
@Service
@Transactional
public class SyncCalendarEventsService implements SyncCalendarEventsUseCase {

  private static final Logger log = LoggerFactory.getLogger(SyncCalendarEventsService.class);
  private final CalendarSyncStateRepository repository;

  public SyncCalendarEventsService(CalendarSyncStateRepository repository) {
    this.repository = repository;
  }

  @Override
  public CalendarSyncStateDetails sync(UUID tenantId) {
    CalendarSyncState state =
        repository
            .findByProvider(CalendarProvider.GOOGLE_CALENDAR)
            .orElseGet(
                () ->
                    repository.save(
                        CalendarSyncState.active(tenantId, CalendarProvider.GOOGLE_CALENDAR)));
    if (state.lastSyncedAt() == null) {
      state.markStale();
      log.info("Sync state marked STALE for tenant={} (never synced)", tenantId);
    } else {
      state.updateSync(state.syncToken(), Instant.now());
      log.info("Calendar synced for tenant={}", tenantId);
    }
    CalendarSyncState saved = repository.save(state);
    return new CalendarSyncStateDetails(
        saved.id(), saved.tenantId(), saved.provider().name(), saved.status());
  }
}
