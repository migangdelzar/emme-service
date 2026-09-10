package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.FindCalendarEventLinksUseCase;
import com.emme.calendar.application.mapper.CalendarEventLinkApplicationMapper;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for appointment calendar-link queries. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindCalendarEventLinksService implements FindCalendarEventLinksUseCase {

  private final CalendarEventLinkRepository repository;

  @Override
  public List<CalendarEventLinkDetails> findByAppointmentId(UUID appointmentId) {
    return repository.findByAppointmentId(appointmentId).stream()
        .map(CalendarEventLinkApplicationMapper::toResult)
        .toList();
  }
}
