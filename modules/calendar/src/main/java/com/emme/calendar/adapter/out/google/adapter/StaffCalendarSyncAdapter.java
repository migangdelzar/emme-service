package com.emme.calendar.adapter.out.google.adapter;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.adapter.out.persistence.entity.GoogleOAuthTokenEntity;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleOAuthTokenRepository;
import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.CreateCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.FindCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.FindCalendarEventLinksUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinkSyncedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksDeletedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksFailedUseCase;
import com.emme.calendar.configuration.CalendarProperties;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.kernel.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Listens for {@link CalendarSyncRequested} events and executes Google Calendar event CRUD
 * operations using user OAuth tokens.
 *
 * <p>No direct dependency on the calendar module's application services — this adapter executes
 * provider calls from the Modulith event bus.
 */
@Service
@Transactional
public class StaffCalendarSyncAdapter {

  private static final Logger log = LoggerFactory.getLogger(StaffCalendarSyncAdapter.class);
  private static final String EVENTS_URL =
      "https://www.googleapis.com/calendar/v3/calendars/%s/events";
  private static final DateTimeFormatter ISO_INSTANT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private final GoogleOAuthAdapter oauthService;
  private final SpringDataGoogleOAuthTokenRepository tokenRepo;
  private final FindCalendarEventLinkUseCase findCalendarEventLink;
  private final FindCalendarEventLinksUseCase findCalendarEventLinks;
  private final CreateCalendarEventLinkUseCase createCalendarEventLink;
  private final MarkCalendarEventLinkSyncedUseCase markCalendarEventLinkSynced;
  private final MarkCalendarEventLinksDeletedUseCase markCalendarEventLinksDeleted;
  private final MarkCalendarEventLinksFailedUseCase markCalendarEventLinksFailed;
  private final CalendarProperties properties;
  private final RestClient httpClient;
  private final ObjectMapper mapper;

  public StaffCalendarSyncAdapter(
      GoogleOAuthAdapter oauthService,
      SpringDataGoogleOAuthTokenRepository tokenRepo,
      FindCalendarEventLinkUseCase findCalendarEventLink,
      FindCalendarEventLinksUseCase findCalendarEventLinks,
      CreateCalendarEventLinkUseCase createCalendarEventLink,
      MarkCalendarEventLinkSyncedUseCase markCalendarEventLinkSynced,
      MarkCalendarEventLinksDeletedUseCase markCalendarEventLinksDeleted,
      MarkCalendarEventLinksFailedUseCase markCalendarEventLinksFailed,
      CalendarProperties properties,
      ObjectMapper mapper,
      @Qualifier("googleRestClient") RestClient httpClient) {
    this.oauthService = oauthService;
    this.tokenRepo = tokenRepo;
    this.findCalendarEventLink = findCalendarEventLink;
    this.findCalendarEventLinks = findCalendarEventLinks;
    this.createCalendarEventLink = createCalendarEventLink;
    this.markCalendarEventLinkSynced = markCalendarEventLinkSynced;
    this.markCalendarEventLinksDeleted = markCalendarEventLinksDeleted;
    this.markCalendarEventLinksFailed = markCalendarEventLinksFailed;
    this.properties = properties;
    this.httpClient = httpClient;
    this.mapper = mapper;
  }

  @ApplicationModuleListener(id = "calendar.sync-requested.staff")
  public void onCalendarSyncRequested(CalendarSyncRequested event) {
    log.info(
        "Received calendar sync request — action={} appointment={} tenant={}",
        event.action(),
        event.appointmentId(),
        event.tenantId());
    try {
      TenantContextHolder.withTenantAndCorrelation(
          event.tenantId(),
          event.databaseId(),
          "calendar-sync:" + event.eventId(),
          () -> process(event));
    } catch (RuntimeException e) {
      log.error("Calendar sync failed for appointment {}", event.appointmentId(), e);
      throw e;
    }
  }

  private void process(CalendarSyncRequested event) {
    try {
      switch (event.action()) {
        case "CREATE" -> createEvent(event);
        case "UPDATE" -> updateEvent(event);
        case "DELETE" -> deleteEvent(event);
        default -> log.warn("Unknown calendar sync action: {}", event.action());
      }
    } catch (Exception exception) {
      log.error("Calendar sync failed for appointment {}", event.appointmentId(), exception);
      markCalendarEventLinksFailed.markFailed(event.tenantId(), event.appointmentId());
      if (exception instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Calendar sync failed", exception);
    }
  }

  // ---------------------------------------------------------------------------
  // CREATE
  // ---------------------------------------------------------------------------

  private void createEvent(CalendarSyncRequested e) throws Exception {
    // Check for existing link to avoid duplicates
    var existing =
        findCalendarEventLink.find(e.appointmentId(), CalendarProvider.GOOGLE_CALENDAR.name());
    if (existing.isPresent()) {
      log.info(
          "Appointment {} already linked to event {} — skipping CREATE",
          e.appointmentId(),
          existing.get().externalEventId());
      return;
    }

    String token = resolveAccessToken(e.tenantId());
    if (token == null) {
      log.warn(
          "No OAuth token available for tenant {} — cannot create calendar event", e.tenantId());
      markCalendarEventLinksFailed.markFailed(e.tenantId(), e.appointmentId());
      return;
    }

    ObjectNode body = mapper.createObjectNode();
    body.put("summary", e.summary() != null ? e.summary() : "Appointment");
    if (e.description() != null && !e.description().isBlank()) {
      body.put("description", e.description());
    }
    body.putObject("start")
        .put("dateTime", ISO_INSTANT.format(e.startsAt()))
        .put("timeZone", "America/Mexico_City");
    body.putObject("end")
        .put("dateTime", ISO_INSTANT.format(e.endsAt()))
        .put("timeZone", "America/Mexico_City");

    String url = String.format(EVENTS_URL, properties.calendarId());
    try {
      String responseBody =
          httpClient
              .post()
              .uri(url)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(mapper.writeValueAsString(body))
              .retrieve()
              .body(String.class);

      ObjectNode created = (ObjectNode) mapper.readTree(responseBody == null ? "" : responseBody);
      String eventId = created.get("id").asText();
      String etag = created.has("etag") ? created.get("etag").asText() : null;

      createCalendarEventLink.create(e.tenantId(), e.appointmentId(), "GOOGLE_CALENDAR", eventId);
      markCalendarEventLinkSynced.markSynced(
          e.appointmentId(), CalendarProvider.GOOGLE_CALENDAR.name(), etag);
      log.info("Created Google Calendar event {} for appointment {}", eventId, e.appointmentId());
    } catch (RuntimeException exception) {
      log.error("Google Calendar event CREATE failed", exception);
      throw exception;
    }
  }

  // ---------------------------------------------------------------------------
  // UPDATE
  // ---------------------------------------------------------------------------

  private void updateEvent(CalendarSyncRequested e) throws Exception {
    String externalEventId = e.oldExternalEventId();
    if (externalEventId == null || externalEventId.isBlank()) {
      // Try to find existing link
      var existing =
          findCalendarEventLink.find(e.appointmentId(), CalendarProvider.GOOGLE_CALENDAR.name());
      if (existing.isPresent()) {
        externalEventId = existing.get().externalEventId();
      } else {
        log.warn(
            "No existing calendar event link for appointment {} — cannot UPDATE",
            e.appointmentId());
        markCalendarEventLinksFailed.markFailed(e.tenantId(), e.appointmentId());
        return;
      }
    }

    String token = resolveAccessToken(e.tenantId());
    if (token == null) {
      log.warn(
          "No OAuth token available for tenant {} — cannot update calendar event", e.tenantId());
      markCalendarEventLinksFailed.markFailed(e.tenantId(), e.appointmentId());
      return;
    }

    ObjectNode body = mapper.createObjectNode();
    body.put("summary", e.summary() != null ? e.summary() : "Appointment");
    if (e.description() != null && !e.description().isBlank()) {
      body.put("description", e.description());
    }
    body.putObject("start")
        .put("dateTime", ISO_INSTANT.format(e.startsAt()))
        .put("timeZone", "America/Mexico_City");
    body.putObject("end")
        .put("dateTime", ISO_INSTANT.format(e.endsAt()))
        .put("timeZone", "America/Mexico_City");

    String url = String.format(EVENTS_URL, properties.calendarId()) + "/" + externalEventId;
    try {
      String responseBody =
          httpClient
              .put()
              .uri(url)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(mapper.writeValueAsString(body))
              .retrieve()
              .body(String.class);

      ObjectNode updated = (ObjectNode) mapper.readTree(responseBody == null ? "" : responseBody);
      String etag = updated.has("etag") ? updated.get("etag").asText() : null;

      if (etag != null) {
        markCalendarEventLinkSynced.markSynced(
            e.appointmentId(), CalendarProvider.GOOGLE_CALENDAR.name(), etag);
      }
      log.info(
          "Updated Google Calendar event {} for appointment {}",
          externalEventId,
          e.appointmentId());
    } catch (RuntimeException exception) {
      log.error("Google Calendar event UPDATE failed for {}", externalEventId, exception);
      throw exception;
    }
  }

  // ---------------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------------

  private void deleteEvent(CalendarSyncRequested e) throws Exception {
    List<CalendarEventLinkDetails> links =
        findCalendarEventLinks.findByAppointmentId(e.appointmentId());
    if (links.isEmpty()) {
      log.warn(
          "No calendar event links found for appointment {} — nothing to DELETE",
          e.appointmentId());
      return;
    }

    String token = resolveAccessToken(e.tenantId());
    if (token == null) {
      log.warn(
          "No OAuth token available for tenant {} — cannot delete calendar events", e.tenantId());
      markCalendarEventLinksFailed.markFailed(e.tenantId(), e.appointmentId());
      return;
    }

    for (CalendarEventLinkDetails link : links) {
      String url =
          String.format(EVENTS_URL, properties.calendarId()) + "/" + link.externalEventId();
      httpClient
          .delete()
          .uri(url)
          .header("Authorization", "Bearer " + token)
          .exchange(
              (request, response) -> {
                if (response.getStatusCode().is2xxSuccessful()
                    || response.getStatusCode().value() == 410) {
                  markCalendarEventLinksDeleted.markDeleted(e.tenantId(), e.appointmentId());
                  log.info(
                      "Deleted Google Calendar event {} (appointment {})",
                      link.externalEventId(),
                      e.appointmentId());
                  return null;
                }
                log.error(
                    "Google Calendar event DELETE failed for {}: HTTP {}",
                    link.externalEventId(),
                    response.getStatusCode().value());
                throw new IllegalStateException(
                    "Google Calendar event DELETE failed with HTTP "
                        + response.getStatusCode().value());
              });
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Resolve a valid OAuth access token for the tenant by finding the first available STAFF token
   * and refreshing if necessary.
   */
  private String resolveAccessToken(UUID tenantId) {
    List<GoogleOAuthTokenEntity> tokens = tokenRepo.findAll();
    for (GoogleOAuthTokenEntity token : tokens) {
      if (token.getPersonaType() == PersonaType.STAFF) {
        try {
          return oauthService.getValidAccessToken(tenantId, token.getUserId(), PersonaType.STAFF);
        } catch (Exception ex) {
          log.warn(
              "Failed to get valid access token for user {}: {}",
              token.getUserId(),
              ex.getMessage());
        }
      }
    }
    return null;
  }
}
