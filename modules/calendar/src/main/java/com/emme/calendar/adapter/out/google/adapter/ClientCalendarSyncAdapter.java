package com.emme.calendar.adapter.out.google.adapter;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.api.result.CalendarEventLinkInfo;
import com.emme.calendar.api.usecase.CreateCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.FindCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinkSyncedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksDeletedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksFailedUseCase;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Syncs a client's own appointments to their personal Google Calendar.
 *
 * <p>Uses {@code PersonaType.CLIENT} when obtaining OAuth tokens and targets the user's {@code
 * primary} calendar. Event links are tracked via focused Calendar use cases so that unsync can
 * locate and delete the corresponding Google Calendar event.
 */
@Service
public class ClientCalendarSyncAdapter {

  private static final Logger log = LoggerFactory.getLogger(ClientCalendarSyncAdapter.class);
  private static final String EVENTS_URL =
      "https://www.googleapis.com/calendar/v3/calendars/primary/events";
  private static final MediaType JSON = MediaType.get("application/json");
  private static final DateTimeFormatter ISO_INSTANT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private final GoogleOAuthAdapter oauthService;
  private final FindCalendarEventLinkUseCase findCalendarEventLink;
  private final CreateCalendarEventLinkUseCase createCalendarEventLink;
  private final MarkCalendarEventLinkSyncedUseCase markCalendarEventLinkSynced;
  private final MarkCalendarEventLinksDeletedUseCase markCalendarEventLinksDeleted;
  private final MarkCalendarEventLinksFailedUseCase markCalendarEventLinksFailed;
  private final OkHttpClient httpClient;
  private final ObjectMapper mapper;

  public ClientCalendarSyncAdapter(
      GoogleOAuthAdapter oauthService,
      FindCalendarEventLinkUseCase findCalendarEventLink,
      CreateCalendarEventLinkUseCase createCalendarEventLink,
      MarkCalendarEventLinkSyncedUseCase markCalendarEventLinkSynced,
      MarkCalendarEventLinksDeletedUseCase markCalendarEventLinksDeleted,
      MarkCalendarEventLinksFailedUseCase markCalendarEventLinksFailed,
      ObjectMapper mapper) {
    this.oauthService = oauthService;
    this.findCalendarEventLink = findCalendarEventLink;
    this.createCalendarEventLink = createCalendarEventLink;
    this.markCalendarEventLinkSynced = markCalendarEventLinkSynced;
    this.markCalendarEventLinksDeleted = markCalendarEventLinksDeleted;
    this.markCalendarEventLinksFailed = markCalendarEventLinksFailed;
    this.httpClient = new OkHttpClient();
    this.mapper = mapper;
  }

  /**
   * Create or update a Google Calendar event for a client appointment in the client's primary
   * calendar.
   *
   * @param tenantId the tenant workspace
   * @param appointmentId the appointment to sync
   * @param startsAt appointment start time (UTC)
   * @param endsAt appointment end time (UTC)
   * @param summary event summary / title
   * @return the Google Calendar event ID
   */
  public String syncAppointment(
      UUID tenantId, UUID appointmentId, Instant startsAt, Instant endsAt, String summary) {
    return syncAppointment(tenantId, appointmentId, startsAt, endsAt, summary, null);
  }

  /**
   * Create or update a Google Calendar event for a client appointment in the client's primary
   * calendar, with optional description.
   *
   * @param tenantId the tenant workspace
   * @param appointmentId the appointment to sync
   * @param startsAt appointment start time (UTC)
   * @param endsAt appointment end time (UTC)
   * @param summary event summary / title
   * @param description optional event description
   * @return the Google Calendar event ID
   */
  public String syncAppointment(
      UUID tenantId,
      UUID appointmentId,
      Instant startsAt,
      Instant endsAt,
      String summary,
      String description) {
    String userId = UserContextHolder.currentSubject();

    log.info(
        "Client calendar sync — tenant={} appointment={} userId={} summary='{}'",
        tenantId,
        appointmentId,
        userId,
        summary);

    // Check for existing link to avoid duplicates
    Optional<CalendarEventLinkInfo> existing = findCalendarEventLink.find(tenantId, appointmentId);
    if (existing.isPresent()) {
      log.info(
          "Appointment {} already linked to event {} — reusing",
          appointmentId,
          existing.get().externalEventId());
      return existing.get().externalEventId();
    }

    String token = oauthService.getValidAccessToken(tenantId, userId, PersonaType.CLIENT);

    ObjectNode body = mapper.createObjectNode();
    body.put("summary", summary != null ? summary : "Appointment");
    if (description != null && !description.isBlank()) {
      body.put("description", description);
    }
    body.putObject("start")
        .put("dateTime", ISO_INSTANT.format(startsAt))
        .put("timeZone", "America/Mexico_City");
    body.putObject("end")
        .put("dateTime", ISO_INSTANT.format(endsAt))
        .put("timeZone", "America/Mexico_City");

    try {
      Request request =
          new Request.Builder()
              .url(EVENTS_URL)
              .header("Authorization", "Bearer " + token)
              .header("Content-Type", "application/json")
              .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
              .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "";
          log.error(
              "Google Calendar event CREATE failed for appointment {}: HTTP {} — {}",
              appointmentId,
              response.code(),
              errorBody);
          throw new RuntimeException(
              "Google Calendar event creation failed: HTTP " + response.code());
        }

        ObjectNode created = (ObjectNode) mapper.readTree(response.body().string());
        String eventId = created.get("id").asText();
        String etag = created.has("etag") ? created.get("etag").asText() : null;

        createCalendarEventLink.create(tenantId, appointmentId, "GOOGLE_CALENDAR", eventId);
        markCalendarEventLinkSynced.markSynced(tenantId, appointmentId, etag);

        log.info(
            "Created Google Calendar event {} for client appointment {}", eventId, appointmentId);
        return eventId;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to sync appointment {} to client calendar", appointmentId, e);
      markCalendarEventLinksFailed.markFailed(tenantId, appointmentId);
      throw new RuntimeException("Failed to sync appointment to Google Calendar", e);
    }
  }

  /**
   * Remove a previously synced appointment from the client's Google Calendar.
   *
   * @param tenantId the tenant workspace
   * @param appointmentId the appointment to remove
   */
  public void unsyncAppointment(UUID tenantId, UUID appointmentId) {
    String userId = UserContextHolder.currentSubject();

    log.info(
        "Client calendar unsync — tenant={} appointment={} userId={}",
        tenantId,
        appointmentId,
        userId);

    Optional<CalendarEventLinkInfo> existing = findCalendarEventLink.find(tenantId, appointmentId);
    if (existing.isEmpty()) {
      log.warn(
          "No calendar event link found for appointment {} — nothing to unsync", appointmentId);
      return;
    }

    String eventId = existing.get().externalEventId();
    String token = oauthService.getValidAccessToken(tenantId, userId, PersonaType.CLIENT);

    try {
      String url = EVENTS_URL + "/" + eventId;
      Request request =
          new Request.Builder()
              .url(url)
              .header("Authorization", "Bearer " + token)
              .delete()
              .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (response.isSuccessful() || response.code() == 410) {
          markCalendarEventLinksDeleted.markDeleted(tenantId, appointmentId);
          log.info(
              "Deleted Google Calendar event {} for client appointment {}", eventId, appointmentId);
        } else {
          String errorBody = response.body() != null ? response.body().string() : "";
          log.error(
              "Google Calendar event DELETE failed for {}: HTTP {} — {}",
              eventId,
              response.code(),
              errorBody);
          markCalendarEventLinksFailed.markFailed(tenantId, appointmentId);
          throw new RuntimeException(
              "Google Calendar event deletion failed: HTTP " + response.code());
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to unsync appointment {} from client calendar", appointmentId, e);
      markCalendarEventLinksFailed.markFailed(tenantId, appointmentId);
      throw new RuntimeException("Failed to unsync appointment from Google Calendar", e);
    }
  }
}
