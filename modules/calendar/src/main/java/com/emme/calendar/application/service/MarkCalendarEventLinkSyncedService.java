package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.MarkCalendarEventLinkSyncedUseCase;
import com.emme.calendar.application.mapper.CalendarEventLinkApplicationMapper;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for marking one calendar link as synchronized. */
@Service
@Transactional
public class MarkCalendarEventLinkSyncedService implements MarkCalendarEventLinkSyncedUseCase {

  private final CalendarEventLinkRepository repository;

  public MarkCalendarEventLinkSyncedService(CalendarEventLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public CalendarEventLinkDetails markSynced(
      UUID appointmentId, CalendarProvider provider, String etag) {
    CalendarEventLink link =
        repository
            .findByAppointmentIdAndProvider(appointmentId, provider)
            .orElseThrow(() -> new IllegalArgumentException("No link found for " + appointmentId));
    link.markSynced(etag);
    return CalendarEventLinkApplicationMapper.toResult(repository.save(link));
  }
}
