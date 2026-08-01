package com.emme.calendar.adapter.out.google.adapter;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.adapter.out.persistence.entity.GoogleOAuthTokenEntity;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleOAuthTokenRepository;
import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.calendar.api.result.CalendarEventLinkInfo;
import com.emme.calendar.api.usecase.CalendarSyncApi;
import com.emme.calendar.configuration.CalendarProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for {@link CalendarSyncRequested} events and executes Google Calendar event CRUD
 * operations using user OAuth tokens.
 *
 * <p>No direct dependency on the calendar module's {@code CalendarService} stubs — this service
 * replaces the TODO stubs with real API calls via the Modulith event bus.
 */
@Service
@Transactional
public class StaffCalendarSyncAdapter {

  private static final Logger log = LoggerFactory.getLogger(StaffCalendarSyncAdapter.class);
  private static final String EVENTS_URL =
      "https://www.googleapis.com/calendar/v3/calendars/%s/events";
  private static final MediaType JSON = MediaType.get("application/json");
  private static final DateTimeFormatter ISO_INSTANT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private final GoogleOAuthAdapter oauthService;
  private final SpringDataGoogleOAuthTokenRepository tokenRepo;
  private final CalendarSyncApi syncApi;
  private final CalendarProperties properties;
  private final OkHttpClient httpClient;
  private final ObjectMapper mapper;

  public StaffCalendarSyncAdapter(
      GoogleOAuthAdapter oauthService,
      SpringDataGoogleOAuthTokenRepository tokenRepo,
      CalendarSyncApi syncApi,
      CalendarProperties properties,
      ObjectMapper mapper) {
    this.oauthService = oauthService;
    this.tokenRepo = tokenRepo;
    this.syncApi = syncApi;
    this.properties = properties;
    this.httpClient = new OkHttpClient();
    this.mapper = mapper;
  }

  @ApplicationModuleListener
  public void onCalendarSyncRequested(CalendarSyncRequested event) {
    log.info(
        "Received calendar sync request — action={} appointment={} tenant={}",
        event.action(),
        event.appointmentId(),
        event.tenantId());
    try {
      switch (event.action()) {
        case "CREATE" -> createEvent(event);
        case "UPDATE" -> updateEvent(event);
        case "DELETE" -> deleteEvent(event);
        default -> log.warn("Unknown calendar sync action: {}", event.action());
      }
    } catch (Exception e) {
      log.error("Calendar sync failed for appointment {}", event.appointmentId(), e);
      syncApi.markFailed(event.tenantId(), event.appointmentId());
    }
  }

  // ---------------------------------------------------------------------------
  // CREATE
  // ---------------------------------------------------------------------------

  private void createEvent(CalendarSyncRequested e) throws Exception {
    // Check for existing link to avoid duplicates
    var existing = syncApi.findByTenantIdAndAppointmentId(e.tenantId(), e.appointmentId());
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
      syncApi.markFailed(e.tenantId(), e.appointmentId());
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
    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        log.error("Google Calendar event CREATE failed: HTTP {} — {}", response.code(), errorBody);
        syncApi.markFailed(e.tenantId(), e.appointmentId());
        return;
      }

      ObjectNode created = (ObjectNode) mapper.readTree(response.body().string());
      String eventId = created.get("id").asText();
      String etag = created.has("etag") ? created.get("etag").asText() : null;

      syncApi.createLink(e.tenantId(), e.appointmentId(), "GOOGLE_CALENDAR", eventId);
      syncApi.markSynced(e.tenantId(), e.appointmentId(), etag);
      log.info("Created Google Calendar event {} for appointment {}", eventId, e.appointmentId());
    }
  }

  // ---------------------------------------------------------------------------
  // UPDATE
  // ---------------------------------------------------------------------------

  private void updateEvent(CalendarSyncRequested e) throws Exception {
    String externalEventId = e.oldExternalEventId();
    if (externalEventId == null || externalEventId.isBlank()) {
      // Try to find existing link
      var existing = syncApi.findByTenantIdAndAppointmentId(e.tenantId(), e.appointmentId());
      if (existing.isPresent()) {
        externalEventId = existing.get().externalEventId();
      } else {
        log.warn(
            "No existing calendar event link for appointment {} — cannot UPDATE",
            e.appointmentId());
        syncApi.markFailed(e.tenantId(), e.appointmentId());
        return;
      }
    }

    String token = resolveAccessToken(e.tenantId());
    if (token == null) {
      log.warn(
          "No OAuth token available for tenant {} — cannot update calendar event", e.tenantId());
      syncApi.markFailed(e.tenantId(), e.appointmentId());
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
    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .put(RequestBody.create(mapper.writeValueAsString(body), JSON))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String errorBody = response.body() != null ? response.body().string() : "";
        log.error(
            "Google Calendar event UPDATE failed for {}: HTTP {} — {}",
            externalEventId,
            response.code(),
            errorBody);
        syncApi.markFailed(e.tenantId(), e.appointmentId());
        return;
      }

      ObjectNode updated = (ObjectNode) mapper.readTree(response.body().string());
      String etag = updated.has("etag") ? updated.get("etag").asText() : null;

      if (etag != null) {
        syncApi.markSynced(e.tenantId(), e.appointmentId(), etag);
      }
      log.info(
          "Updated Google Calendar event {} for appointment {}",
          externalEventId,
          e.appointmentId());
    }
  }

  // ---------------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------------

  private void deleteEvent(CalendarSyncRequested e) throws Exception {
    List<CalendarEventLinkInfo> links = syncApi.findByAppointmentId(e.appointmentId());
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
      syncApi.markFailed(e.tenantId(), e.appointmentId());
      return;
    }

    for (CalendarEventLinkInfo link : links) {
      String url =
          String.format(EVENTS_URL, properties.calendarId()) + "/" + link.externalEventId();
      Request request =
          new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + token)
              .delete()
              .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (response.isSuccessful() || response.code() == 410) {
          syncApi.markDeleted(e.tenantId(), e.appointmentId());
          log.info(
              "Deleted Google Calendar event {} (appointment {})",
              link.externalEventId(),
              e.appointmentId());
        } else {
          String errorBody = response.body() != null ? response.body().string() : "";
          log.error(
              "Google Calendar event DELETE failed for {}: HTTP {} — {}",
              link.externalEventId(),
              response.code(),
              errorBody);
          syncApi.markFailed(e.tenantId(), e.appointmentId());
        }
      }
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
    List<GoogleOAuthTokenEntity> tokens = tokenRepo.findByTenantId(tenantId);
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
