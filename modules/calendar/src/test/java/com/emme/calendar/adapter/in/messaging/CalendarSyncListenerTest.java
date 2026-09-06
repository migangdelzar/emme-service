package com.emme.calendar.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.tenancy.application.port.out.TenantRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class CalendarSyncListenerTest {

  @Test
  void publishesSyncRequestWithTheTenantDatabaseRoutingIdentity() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    TenantRepository tenants = mock(TenantRepository.class);
    UUID tenantId = UUID.randomUUID();
    UUID databaseId = UUID.randomUUID();
    when(tenants.findDatabaseIdByTenantId(tenantId)).thenReturn(Optional.of(databaseId));
    CalendarSyncListener listener = new CalendarSyncListener(publisher, tenants);

    listener.onAppointmentCreated(
        new AppointmentCreated(
            UUID.randomUUID(),
            tenantId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-06T10:00:00Z"),
            Instant.parse("2026-09-06T11:00:00Z"),
            Instant.parse("2026-09-06T09:00:00Z")));

    ArgumentCaptor<CalendarSyncRequested> event =
        ArgumentCaptor.forClass(CalendarSyncRequested.class);
    verify(publisher).publishEvent(event.capture());
    assertThat(event.getValue().tenantId()).isEqualTo(tenantId);
    assertThat(event.getValue().databaseId()).isEqualTo(databaseId);
  }
}
