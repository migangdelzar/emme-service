package com.emme.appointments.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.adapter.out.persistence.entity.AppointmentHoldEntity;
import com.emme.appointments.adapter.out.persistence.mapper.AppointmentHoldPersistenceMapper;
import com.emme.appointments.adapter.out.persistence.repository.SpringDataAppointmentHoldRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentHoldPersistenceAdapterTest {

  @Test
  void findsHoldByIdempotencyKeyInTheCurrentTenantSchema() {
    SpringDataAppointmentHoldRepository repository = mock();
    AppointmentHoldPersistenceAdapter adapter =
        new AppointmentHoldPersistenceAdapter(repository, new AppointmentHoldPersistenceMapper());
    AppointmentHoldEntity entity =
        new AppointmentHoldEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2030-01-01T09:15:00Z"),
            "hold-1");
    when(repository.findByIdempotencyKey("hold-1")).thenReturn(Optional.of(entity));

    assertThat(adapter.findByIdempotencyKey("hold-1"))
        .contains(
            new AppointmentHold(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getExpiresAt(),
                entity.getIdempotencyKey()));

    verify(repository).findByIdempotencyKey("hold-1");
  }

  @Test
  void deletesHoldByItsStableIdentifier() {
    SpringDataAppointmentHoldRepository repository = mock();
    AppointmentHoldPersistenceAdapter adapter =
        new AppointmentHoldPersistenceAdapter(repository, new AppointmentHoldPersistenceMapper());
    UUID holdId = UUID.randomUUID();

    adapter.deleteById(holdId);

    verify(repository).deleteById(holdId);
  }

  @Test
  void findsHoldByItsStableIdentifierInTheCurrentTenantSchema() {
    SpringDataAppointmentHoldRepository repository = mock();
    AppointmentHoldPersistenceAdapter adapter =
        new AppointmentHoldPersistenceAdapter(repository, new AppointmentHoldPersistenceMapper());
    AppointmentHoldEntity entity =
        new AppointmentHoldEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2030-01-01T09:15:00Z"),
            "hold-1");
    when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

    assertThat(adapter.findById(entity.getId()))
        .contains(
            new AppointmentHold(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getExpiresAt(),
                entity.getIdempotencyKey()));

    verify(repository).findById(entity.getId());
  }
}
