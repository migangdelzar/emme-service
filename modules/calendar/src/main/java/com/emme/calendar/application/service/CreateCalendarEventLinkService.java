package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.CreateCalendarEventLinkUseCase;
import com.emme.calendar.application.mapper.CalendarEventLinkApplicationMapper;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for creating calendar event links. */
@Service
@Transactional
public class CreateCalendarEventLinkService implements CreateCalendarEventLinkUseCase {

  private final CalendarEventLinkRepository repository;

  public CreateCalendarEventLinkService(CalendarEventLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public CalendarEventLinkDetails create(
      UUID tenantId, UUID appointmentId, String provider, String externalEventId) {
    CalendarEventLink link =
        CalendarEventLink.pending(
            tenantId, appointmentId, CalendarProvider.valueOf(provider), externalEventId);
    return CalendarEventLinkApplicationMapper.toResult(repository.save(link));
  }
}
