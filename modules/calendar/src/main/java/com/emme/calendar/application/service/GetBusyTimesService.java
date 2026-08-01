package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarBusyTimeRange;
import com.emme.calendar.api.usecase.GetBusyTimesUseCase;
import com.emme.calendar.application.port.out.GoogleCalendarPort;
import com.emme.calendar.configuration.CalendarProperties;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for external calendar availability queries. */
@Service
@Transactional(readOnly = true)
public class GetBusyTimesService implements GetBusyTimesUseCase {

  private static final Logger log = LoggerFactory.getLogger(GetBusyTimesService.class);
  private final GoogleCalendarPort googleCalendar;
  private final CalendarProperties properties;

  public GetBusyTimesService(GoogleCalendarPort googleCalendar, CalendarProperties properties) {
    this.googleCalendar = googleCalendar;
    this.properties = properties;
  }

  @Override
  public List<CalendarBusyTimeRange> getBusyTimes(UUID tenantId, UUID artistId, LocalDate date) {
    if (!googleCalendar.isConfigured()) {
      log.info(
          "Google Calendar not configured — returning empty busy times for artist={}, date={}",
          artistId,
          date);
      return Collections.emptyList();
    }
    String timeMin = date.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    String timeMax =
        date.plusDays(1).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    log.info(
        "Fetching busy times for artist={}, date={}, calendar={}",
        artistId,
        date,
        properties.calendarId());
    return googleCalendar.freeBusy(properties.calendarId(), timeMin, timeMax);
  }
}
