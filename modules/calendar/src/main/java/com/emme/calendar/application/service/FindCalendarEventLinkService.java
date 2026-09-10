package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.FindCalendarEventLinkUseCase;
import com.emme.calendar.application.mapper.CalendarEventLinkApplicationMapper;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for tenant-scoped calendar-link queries. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindCalendarEventLinkService implements FindCalendarEventLinkUseCase {

  private final CalendarEventLinkRepository repository;

  @Override
  public Optional<CalendarEventLinkDetails> find(UUID appointmentId, String provider) {
    return repository
        .findByAppointmentIdAndProvider(appointmentId, CalendarProvider.valueOf(provider))
        .map(CalendarEventLinkApplicationMapper::toResult);
  }
}
