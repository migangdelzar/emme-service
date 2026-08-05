package com.emme.appointments.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.domain.model.Appointment;
import com.emme.appointments.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentPersistenceMapperTest {

  @Test
  void mapsNewAndManagedEntitiesWithoutMovingJpaTypesIntoTheDomain() {
    UUID tenantId = UUID.randomUUID();
    Appointment domain = appointment(tenantId);
    AppointmentPersistenceMapper mapper = new AppointmentPersistenceMapper();

    AppointmentEntity entity = mapper.toNewEntity(domain);

    assertThat(entity.getCustomerId()).isEqualTo(domain.getCustomerId());
    assertThat(entity.getServiceId()).isEqualTo(domain.getServiceId());
    assertThat(entity.getArtistId()).isEqualTo(domain.getArtistId());
    assertThat(entity.getStartsAt()).isEqualTo(domain.getStartsAt());

    domain.reschedule(Instant.parse("2026-08-01T12:00:00Z"), Instant.parse("2026-08-01T13:00:00Z"));
    domain.cancel();
    mapper.updateEntity(domain, entity);

    assertThat(entity.getStartsAt()).isEqualTo(domain.getStartsAt());
    assertThat(entity.getEndsAt()).isEqualTo(domain.getEndsAt());
    assertThat(entity.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
  }

  private static Appointment appointment(UUID tenantId) {
    return new Appointment(
        tenantId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-08-01T10:00:00Z"),
        Instant.parse("2026-08-01T11:00:00Z"));
  }
}
