package com.emme.studio.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.domain.model.Appointment;
import com.emme.studio.domain.model.AppointmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentPersistenceMapperTest {

  @Test
  void mapsNewAndManagedEntitiesWithoutMovingJpaTypesIntoTheDomain() {
    UUID tenantId = UUID.randomUUID();
    CustomerEntity customer = new CustomerEntity(tenantId, "Ada");
    ServiceEntity service = new ServiceEntity(tenantId, "CUT", "Cut", 60, BigDecimal.TEN);
    ArtistEntity artist = new ArtistEntity(tenantId, "Alex");
    Appointment domain = appointment(tenantId);
    AppointmentPersistenceMapper mapper = new AppointmentPersistenceMapper();

    AppointmentEntity entity = mapper.toNewEntity(domain, customer, service, artist);

    assertThat(entity.getCustomer()).isSameAs(customer);
    assertThat(entity.getService()).isSameAs(service);
    assertThat(entity.getArtist()).isSameAs(artist);
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
