package com.emme.calendar.application.service;

import com.emme.calendar.api.usecase.MarkCalendarEventLinksDeletedUseCase;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for deleting all links belonging to an appointment. */
@Service
@Transactional
public class MarkCalendarEventLinksDeletedService implements MarkCalendarEventLinksDeletedUseCase {

  private final CalendarEventLinkRepository repository;

  public MarkCalendarEventLinksDeletedService(CalendarEventLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public void markDeleted(UUID tenantId, UUID appointmentId) {
    var links = repository.findByAppointmentId(appointmentId);
    links.forEach(link -> link.markDeleted());
    repository.saveAll(links);
  }
}
