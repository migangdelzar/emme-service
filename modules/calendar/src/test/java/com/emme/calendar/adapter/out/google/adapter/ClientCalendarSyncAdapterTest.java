package com.emme.calendar.adapter.out.google.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.api.result.CalendarEventLinkDetails;
import com.emme.calendar.api.usecase.CreateCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.FindCalendarEventLinkUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinkSyncedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksDeletedUseCase;
import com.emme.calendar.api.usecase.MarkCalendarEventLinksFailedUseCase;
import com.emme.calendar.domain.model.CalendarProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ClientCalendarSyncAdapterTest {

  @Test
  void reusesAnExistingEventLinkWithoutCallingTheProvider() {
    UUID appointmentId = UUID.randomUUID();
    String externalEventId = "event-123";
    FindCalendarEventLinkUseCase findEventLink = mock(FindCalendarEventLinkUseCase.class);
    when(findEventLink.find(appointmentId, CalendarProvider.GOOGLE_CALENDAR.name()))
        .thenReturn(
            Optional.of(
                new CalendarEventLinkDetails(
                    UUID.randomUUID(),
                    appointmentId,
                    CalendarProvider.GOOGLE_CALENDAR.name(),
                    externalEventId,
                    "etag-1",
                    "SYNCED")));

    ClientCalendarSyncAdapter adapter =
        new ClientCalendarSyncAdapter(
            mock(GoogleOAuthAdapter.class),
            findEventLink,
            mock(CreateCalendarEventLinkUseCase.class),
            mock(MarkCalendarEventLinkSyncedUseCase.class),
            mock(MarkCalendarEventLinksDeletedUseCase.class),
            mock(MarkCalendarEventLinksFailedUseCase.class),
            new ObjectMapper(),
            RestClient.builder().build());

    assertThat(
            adapter.sync(
                UUID.randomUUID(),
                appointmentId,
                "user-123",
                java.time.Instant.parse("2026-09-05T10:00:00Z"),
                java.time.Instant.parse("2026-09-05T11:00:00Z"),
                "Appointment",
                null))
        .isEqualTo(externalEventId);
  }

  @Test
  void treatsAnAlreadyGoneProviderEventAsSuccessfullyUnsynced() {
    UUID tenantId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    FindCalendarEventLinkUseCase findEventLink = mock(FindCalendarEventLinkUseCase.class);
    when(findEventLink.find(appointmentId, CalendarProvider.GOOGLE_CALENDAR.name()))
        .thenReturn(
            Optional.of(
                new CalendarEventLinkDetails(
                    UUID.randomUUID(),
                    appointmentId,
                    CalendarProvider.GOOGLE_CALENDAR.name(),
                    "event-123",
                    "etag-1",
                    "SYNCED")));
    GoogleOAuthAdapter oauth = mock(GoogleOAuthAdapter.class);
    when(oauth.getValidAccessToken(tenantId, "user-123", PersonaType.CLIENT))
        .thenReturn("google-token");
    MarkCalendarEventLinksDeletedUseCase deleted = mock(MarkCalendarEventLinksDeletedUseCase.class);
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    server
        .expect(
            requestTo("https://www.googleapis.com/calendar/v3/calendars/primary/events/event-123"))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header("Authorization", "Bearer google-token"))
        .andRespond(withStatus(HttpStatus.GONE));

    ClientCalendarSyncAdapter adapter =
        new ClientCalendarSyncAdapter(
            oauth,
            findEventLink,
            mock(CreateCalendarEventLinkUseCase.class),
            mock(MarkCalendarEventLinkSyncedUseCase.class),
            deleted,
            mock(MarkCalendarEventLinksFailedUseCase.class),
            new ObjectMapper(),
            builder.build());

    adapter.unsync(tenantId, appointmentId, "user-123");

    org.mockito.Mockito.verify(deleted).markDeleted(tenantId, appointmentId);
    server.verify();
  }
}
