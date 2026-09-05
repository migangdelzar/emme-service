package com.emme.appointments.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.appointments.domain.model.AppointmentStatus;
import com.emme.appointments.domain.model.ExternalCalendarStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentTenantScopedUpdateTest {

  @Test
  void updatesAppointmentThroughTheTenantScopedRepositoryQuery() {
    SpringDataAppointmentRepository repository = org.mockito.Mockito.mock();
    AppointmentPersistenceAdapter adapter = new AppointmentPersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    UUID artistId = UUID.randomUUID();
    Instant originalStart = Instant.parse("2026-09-04T15:00:00Z");
    Instant originalEnd = Instant.parse("2026-09-04T16:00:00Z");
    Instant updatedStart = Instant.parse("2026-09-04T17:00:00Z");
    Instant updatedEnd = Instant.parse("2026-09-04T18:00:00Z");
    AppointmentEntity entity =
        new AppointmentEntity(
            tenantId, customerId, serviceId, artistId, originalStart, originalEnd);
    entity.onCreate();
    Appointment appointment =
        Appointment.reconstitute(
            appointmentId,
            tenantId,
            customerId,
            serviceId,
            artistId,
            updatedStart,
            updatedEnd,
            AppointmentStatus.CONFIRMED,
            ExternalCalendarStatus.NOT_SYNCED);
    when(repository.findByTenantIdAndId(tenantId, appointmentId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    Appointment saved = adapter.save(appointment);

    verify(repository).findByTenantIdAndId(tenantId, appointmentId);
    verify(repository, never()).findById(appointmentId);
    assertThat(entity.getStartsAt()).isEqualTo(updatedStart);
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
  }
}
