package com.emme.appointments.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.application.port.out.AppointmentRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentCollisionAdapterTest {

  @Test
  void appointmentForSameArtistInAnotherTenantDoesNotCauseCollision() {
    UUID tenant = UUID.randomUUID();
    UUID artist = UUID.randomUUID();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    AppointmentRepository repository = mock(AppointmentRepository.class);
    when(repository.existsActiveCollision(tenant, artist, start, end, null)).thenReturn(false);

    AppointmentCollisionAdapter adapter = new AppointmentCollisionAdapter(repository);

    assertThat(adapter.hasCollision(tenant, artist, start, end, null)).isFalse();
    verify(repository).existsActiveCollision(tenant, artist, start, end, null);
  }

  @Test
  void activeAppointmentCausesTenantScopedCollision() {
    UUID tenant = UUID.randomUUID();
    UUID artist = UUID.randomUUID();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    AppointmentRepository repository = mock(AppointmentRepository.class);
    when(repository.existsActiveCollision(tenant, artist, start, end, null)).thenReturn(true);

    AppointmentCollisionAdapter adapter = new AppointmentCollisionAdapter(repository);

    assertThat(adapter.hasCollision(tenant, artist, start, end, null)).isTrue();
    verify(repository).existsActiveCollision(tenant, artist, start, end, null);
  }

  @Test
  void excludedAppointmentIsPassedToCollisionQueryWhenRescheduling() {
    UUID tenant = UUID.randomUUID();
    UUID artist = UUID.randomUUID();
    UUID excludedAppointment = UUID.randomUUID();
    Instant start = Instant.parse("2030-01-01T10:00:00Z");
    Instant end = Instant.parse("2030-01-01T11:00:00Z");
    AppointmentRepository repository = mock(AppointmentRepository.class);
    when(repository.existsActiveCollision(tenant, artist, start, end, excludedAppointment))
        .thenReturn(false);

    AppointmentCollisionAdapter adapter = new AppointmentCollisionAdapter(repository);

    assertThat(adapter.hasCollision(tenant, artist, start, end, excludedAppointment)).isFalse();
    verify(repository).existsActiveCollision(tenant, artist, start, end, excludedAppointment);
  }
}
