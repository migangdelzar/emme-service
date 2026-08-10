package com.emme.calendar.application.service;

import com.emme.calendar.api.usecase.MarkCalendarEventLinksFailedUseCase;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for failing all links belonging to an appointment. */
@Service
@Transactional
public class MarkCalendarEventLinksFailedService implements MarkCalendarEventLinksFailedUseCase {

  private final CalendarEventLinkRepository repository;

  public MarkCalendarEventLinksFailedService(CalendarEventLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public void markFailed(UUID tenantId, UUID appointmentId) {
    var links = repository.findByAppointmentId(appointmentId);
    links.forEach(link -> link.markFailed());
    repository.saveAll(links);
  }
}
