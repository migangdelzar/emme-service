package com.emme.calendar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.entity.CalendarEventLink;
import com.emme.calendar.entity.CalendarEventLinkRepository;
import com.emme.calendar.entity.CalendarProvider;
import com.emme.calendar.entity.CalendarSyncState;
import com.emme.calendar.entity.CalendarSyncStateRepository;
import com.emme.calendar.entity.CalendarSyncStatus;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class CalendarRepositoryTest extends BaseRepositoryTest {

  @Autowired private CalendarSyncStateRepository syncStateRepo;

  @Autowired private CalendarEventLinkRepository eventLinkRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void shouldSaveAndFindSyncState() {
    CalendarSyncState state = new CalendarSyncState(TENANT_ID, CalendarProvider.GOOGLE_CALENDAR);
    CalendarSyncState saved = syncStateRepo.save(state);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(CalendarSyncStatus.ACTIVE);

    List<CalendarSyncState> found = syncStateRepo.findByTenantId(TENANT_ID);
    assertThat(found).isNotEmpty();
  }

  @Test
  void shouldFindEventLinks() {
    UUID appointmentId = UUID.randomUUID();
    CalendarEventLink link =
        new CalendarEventLink(
            TENANT_ID, appointmentId, CalendarProvider.GOOGLE_CALENDAR, "ext-event-123");
    CalendarEventLink saved = eventLinkRepo.save(link);

    assertThat(saved.getId()).isNotNull();

    List<CalendarEventLink> byAppointment = eventLinkRepo.findByAppointmentId(appointmentId);
    assertThat(byAppointment).hasSize(1);
    assertThat(byAppointment.get(0).getExternalEventId()).isEqualTo("ext-event-123");
  }
}
