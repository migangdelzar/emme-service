package com.emme.calendar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.adapter.out.persistence.entity.CalendarEventLinkEntity;
import com.emme.calendar.adapter.out.persistence.entity.CalendarSyncStateEntity;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarEventLinkRepository;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataCalendarSyncStateRepository;
import com.emme.calendar.domain.model.CalendarProvider;
import com.emme.calendar.domain.model.CalendarSyncStatus;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class CalendarRepositoryTest extends BaseRepositoryTest {

  @Autowired private SpringDataCalendarSyncStateRepository syncStateRepo;

  @Autowired private SpringDataCalendarEventLinkRepository eventLinkRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void shouldSaveAndFindSyncState() {
    CalendarSyncStateEntity state =
        new CalendarSyncStateEntity(TENANT_ID, CalendarProvider.GOOGLE_CALENDAR);
    CalendarSyncStateEntity saved = syncStateRepo.save(state);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(CalendarSyncStatus.ACTIVE);

    List<CalendarSyncStateEntity> found = syncStateRepo.findByTenantId(TENANT_ID);
    assertThat(found).isNotEmpty();
  }

  @Test
  void shouldFindEventLinks() {
    UUID appointmentId = UUID.randomUUID();
    CalendarEventLinkEntity link =
        new CalendarEventLinkEntity(
            TENANT_ID, appointmentId, CalendarProvider.GOOGLE_CALENDAR, "ext-event-123");
    CalendarEventLinkEntity saved = eventLinkRepo.save(link);

    assertThat(saved.getId()).isNotNull();

    List<CalendarEventLinkEntity> byAppointment = eventLinkRepo.findByAppointmentId(appointmentId);
    assertThat(byAppointment).hasSize(1);
    assertThat(byAppointment.get(0).getExternalEventId()).isEqualTo("ext-event-123");
  }
}
