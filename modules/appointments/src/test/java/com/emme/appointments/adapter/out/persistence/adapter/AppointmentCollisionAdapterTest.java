package com.emme.appointments.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.appointments.application.port.out.AppointmentRepository;
import java.time.Instant;
import java.util.List;
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
    when(repository.findByTenantIdAndArtistIdAndOverlappingInterval(tenant, artist, start, end))
        .thenReturn(List.of());

    AppointmentCollisionAdapter adapter = new AppointmentCollisionAdapter(repository);

    assertThat(adapter.hasCollision(tenant, artist, start, end, null)).isFalse();
  }
}
