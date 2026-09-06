package com.emme.calendar.adapter.out.google.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleOAuthTokenRepository;
import com.emme.calendar.api.event.CalendarSyncRequested;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class StaffCalendarSyncAdapterTest {

  @Test
  void restoresTheEventTenantBeforeUsingTenantScopedPersistence() {
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    FindCalendarEventLinkUseCase findEventLink = mock(FindCalendarEventLinkUseCase.class);
    when(findEventLink.find(appointmentId, CalendarProvider.GOOGLE_CALENDAR.name()))
        .thenAnswer(
            invocation -> {
              assertThat(TenantContextHolder.currentTenantOptional()).contains(tenantId);
              assertThat(TenantContextHolder.currentDatabaseOptional()).contains(databaseId);
              return Optional.empty();
            });
    SpringDataGoogleOAuthTokenRepository tokenRepository =
        mock(SpringDataGoogleOAuthTokenRepository.class);
    when(tokenRepository.findAll()).thenReturn(List.of());

    StaffCalendarSyncAdapter adapter =
        new StaffCalendarSyncAdapter(
            mock(GoogleOAuthAdapter.class),
            tokenRepository,
            findEventLink,
            mock(FindCalendarEventLinksUseCase.class),
            mock(CreateCalendarEventLinkUseCase.class),
            mock(MarkCalendarEventLinkSyncedUseCase.class),
            mock(MarkCalendarEventLinksDeletedUseCase.class),
            mock(MarkCalendarEventLinksFailedUseCase.class),
            mock(CalendarProperties.class),
            new ObjectMapper(),
            RestClient.builder().build());

    adapter.onCalendarSyncRequested(
        new CalendarSyncRequested(
            tenantId,
            databaseId,
            appointmentId,
            "CREATE",
            "Appointment",
            null,
            Instant.parse("2026-09-05T10:00:00Z"),
            Instant.parse("2026-09-05T11:00:00Z"),
            null));

    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
  }
}
