package com.emme.calendar.application.service;

import com.emme.calendar.api.result.CalendarBusyTimeRange;
import com.emme.calendar.application.port.out.CalendarEventLinkRepository;
import com.emme.calendar.application.port.out.CalendarSyncStateRepository;
import com.emme.calendar.application.port.out.GoogleCalendarPort;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncState;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalendarService {

  private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

  private final CalendarSyncStateRepository syncStateRepository;
  private final CalendarEventLinkRepository eventLinkRepository;
  private final GoogleCalendarPort googleCalendarClient;

  @Value("${app.calendar.calendar-id:primary}")
  private String calendarId;

  public CalendarService(
      CalendarSyncStateRepository syncStateRepository,
      CalendarEventLinkRepository eventLinkRepository,
      GoogleCalendarPort googleCalendarClient) {
    this.syncStateRepository = syncStateRepository;
    this.eventLinkRepository = eventLinkRepository;
    this.googleCalendarClient = googleCalendarClient;
  }

  /** Get busy times for an artist on a given date via Google Calendar free/busy API. */
  @Transactional(readOnly = true)
  public List<CalendarBusyTimeRange> getBusyTimes(UUID tenantId, UUID artistId, LocalDate date) {
    if (!googleCalendarClient.isConfigured()) {
      log.info(
          "Google Calendar not configured — returning empty busy times for artist={}, date={}",
          artistId,
          date);
      return Collections.emptyList();
    }
    String timeMin = date.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    String timeMax =
        date.plusDays(1).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    log.info("Fetching busy times for artist={}, date={}, calendar={}", artistId, date, calendarId);
    return googleCalendarClient.freeBusy(calendarId, timeMin, timeMax);
  }

  /** Trigger calendar sync for a tenant. Stub: marks STALE if never synced. */
  public CalendarSyncState syncEvents(UUID tenantId) {
    CalendarSyncState state =
        syncStateRepository
            .findByTenantIdAndProvider(tenantId, CalendarProvider.GOOGLE_CALENDAR)
            .orElseGet(
                () -> {
                  CalendarSyncState newState =
                      CalendarSyncState.active(tenantId, CalendarProvider.GOOGLE_CALENDAR);
                  return syncStateRepository.save(newState);
                });

    if (state.lastSyncedAt() == null) {
      state.markStale();
      log.info("Sync state marked STALE for tenant={} (never synced)", tenantId);
    } else {
      // Stub: successful sync
      state.updateSync(state.syncToken(), Instant.now());
      log.info("Calendar synced for tenant={}", tenantId);
    }

    return syncStateRepository.save(state);
  }

  /** Get all calendar event links for an appointment */
  @Transactional(readOnly = true)
  public List<CalendarEventLink> getLinks(UUID appointmentId) {
    log.info("Fetching calendar links for appointment={}", appointmentId);
    return eventLinkRepository.findByAppointmentId(appointmentId);
  }

  /** Link an appointment to an external calendar event. Stub: PENDING → SYNCED */
  public CalendarEventLink linkAppointment(UUID appointmentId, String externalEventId) {
    // In a real implementation, tenantId would come from context or the appointment
    // For stub purposes, we create a link without tenant context validation
    CalendarEventLink link =
        eventLinkRepository
            .findByTenantIdAndAppointmentId(UUID.randomUUID(), appointmentId)
            .orElse(null);

    if (link != null) {
      log.info(
          "Appointment {} already linked to external event {}", appointmentId, externalEventId);
      return link;
    }

    // Create new link — tenantId would normally be resolved from appointment
    CalendarEventLink newLink =
        CalendarEventLink.pending(
            UUID.randomUUID(), appointmentId, CalendarProvider.GOOGLE_CALENDAR, externalEventId);
    newLink.markSynced(null);
    log.info("Appointment {} linked to external event {}", appointmentId, externalEventId);
    return eventLinkRepository.save(newLink);
  }

  /** Sync new appointment to external calendar. Stub: creates PENDING→SYNCED link. */
  public void syncNewAppointment(
      UUID tenantId, UUID appointmentId, Instant startsAt, Instant endsAt) {
    // TODO: Google Calendar API — create event
    // POST https://www.googleapis.com/calendar/v3/calendars/primary/events
    CalendarEventLink link =
        CalendarEventLink.pending(
            tenantId,
            appointmentId,
            CalendarProvider.GOOGLE_CALENDAR,
            "stub_event_" + appointmentId);
    link.markSynced(null);
    eventLinkRepository.save(link);
    log.info("Synced new appointment {} to calendar for tenant={}", appointmentId, tenantId);
  }

  /** Unsync appointment from external calendar. Stub: marks links as DELETED. */
  public void unsyncAppointment(UUID tenantId, UUID appointmentId) {
    // TODO: Google Calendar API — delete event
    List<CalendarEventLink> links = eventLinkRepository.findByAppointmentId(appointmentId);
    links.forEach(
        link -> {
          link.markDeleted();
          eventLinkRepository.save(link);
          log.info(
              "Marked calendar link {} as DELETED for appointment {}", link.id(), appointmentId);
        });
  }

  /** Resync appointment in external calendar. Stub: unlink + relink. */
  public void resyncAppointment(
      UUID tenantId, UUID appointmentId, Instant newStartsAt, Instant newEndsAt) {
    // TODO: Google Calendar API — update event
    unsyncAppointment(tenantId, appointmentId);
    syncNewAppointment(tenantId, appointmentId, newStartsAt, newEndsAt);
    log.info("Resynced appointment {} for tenant={}", appointmentId, tenantId);
  }
}
