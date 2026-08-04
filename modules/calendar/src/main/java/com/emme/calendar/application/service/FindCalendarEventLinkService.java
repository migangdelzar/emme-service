package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.FindCalendarEventLinkUseCase;
import com.emme.calendar.application.mapper.CalendarEventLinkApplicationMapper;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for tenant-scoped calendar-link queries. */
@Service
@Transactional(readOnly = true)
public class FindCalendarEventLinkService implements FindCalendarEventLinkUseCase {

  private final CalendarEventLinkRepository repository;

  public FindCalendarEventLinkService(CalendarEventLinkRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<CalendarEventLinkDetails> find(UUID tenantId, UUID appointmentId) {
    return repository
        .findByTenantIdAndAppointmentId(tenantId, appointmentId)
        .map(CalendarEventLinkApplicationMapper::toResult);
  }
}
