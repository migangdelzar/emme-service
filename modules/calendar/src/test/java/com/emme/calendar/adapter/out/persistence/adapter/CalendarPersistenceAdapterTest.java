package com.emme.calendar.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.calendar.adapter.out.persistence.entity.CalendarEventLinkEntity;
import com.emme.calendar.adapter.out.persistence.mapper.CalendarPersistenceMapper;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarEventLinkRepository;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarSyncStateRepository;
import com.emme.calendar.domain.model.CalendarEventLink;
import com.emme.calendar.domain.model.CalendarProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarPersistenceAdapterTest {

  private final SpringDataCalendarEventLinkRepository eventLinks =
      mock(SpringDataCalendarEventLinkRepository.class);
  private final SpringDataCalendarSyncStateRepository syncStates =
      mock(SpringDataCalendarSyncStateRepository.class);
  private final CalendarPersistenceAdapter adapter =
      new CalendarPersistenceAdapter(eventLinks, syncStates, new CalendarPersistenceMapper());

  @Test
  void savesDomainLinkAndReturnsMappedDomainLink() {
    CalendarEventLink link =
        CalendarEventLink.pending(
            UUID.randomUUID(), UUID.randomUUID(), CalendarProvider.GOOGLE_CALENDAR, "event-1");
    CalendarEventLinkEntity entity =
        CalendarEventLinkEntity.restore(
            link.id(),
            link.tenantId(),
            link.appointmentId(),
            link.provider(),
            link.externalEventId(),
            link.etag(),
            link.status());
    when(eventLinks.findById(link.id())).thenReturn(Optional.empty());
    when(eventLinks.save(any(CalendarEventLinkEntity.class))).thenReturn(entity);

    CalendarEventLink saved = adapter.save(link);

    assertThat(saved.id()).isEqualTo(link.id());
    assertThat(saved.externalEventId()).isEqualTo("event-1");
  }

  @Test
  void findsOneSchemaLocalLinkByAppointmentAndProvider() {
    CalendarEventLink link =
        CalendarEventLink.pending(
            UUID.randomUUID(), UUID.randomUUID(), CalendarProvider.GOOGLE_CALENDAR, "event-1");
    CalendarEventLinkEntity entity =
        CalendarEventLinkEntity.restore(
            link.id(),
            link.tenantId(),
            link.appointmentId(),
            link.provider(),
            link.externalEventId(),
            link.etag(),
            link.status());
    when(eventLinks.findByAppointmentIdAndProvider(
            link.appointmentId(), CalendarProvider.GOOGLE_CALENDAR))
        .thenReturn(Optional.of(entity));

    assertThat(
            adapter.findByAppointmentIdAndProvider(
                link.appointmentId(), CalendarProvider.GOOGLE_CALENDAR))
        .get()
        .extracting(CalendarEventLink::appointmentId, CalendarEventLink::provider)
        .containsExactly(link.appointmentId(), link.provider());
  }
}
