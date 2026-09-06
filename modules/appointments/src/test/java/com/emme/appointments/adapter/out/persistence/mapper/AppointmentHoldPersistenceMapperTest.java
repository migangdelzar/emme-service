package com.emme.appointments.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.adapter.out.persistence.entity.AppointmentHoldEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

class AppointmentHoldPersistenceMapperTest {

  @Test
  void isACompositionBeanForThePersistenceAdapter() {
    assertThat(AppointmentHoldPersistenceMapper.class.getAnnotation(Component.class)).isNotNull();
  }

  @Test
  void mapsTenantLocalHoldWithoutAddingTenantDataToTheDomainContract() {
    UUID tenantId = UUID.randomUUID();
    AppointmentHold hold =
        new AppointmentHold(
            UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2030-01-01T09:15:00Z"), "hold-1");

    AppointmentHoldPersistenceMapper mapper = new AppointmentHoldPersistenceMapper();
    AppointmentHoldEntity entity = mapper.toNewEntity(hold, tenantId);

    assertThat(entity.getId()).isEqualTo(hold.holdId());
    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getAppointmentId()).isEqualTo(hold.appointmentId());
    assertThat(entity.getExpiresAt()).isEqualTo(hold.expiresAt());
    assertThat(entity.getIdempotencyKey()).isEqualTo(hold.idempotencyKey());
    assertThat(mapper.toDomain(entity)).isEqualTo(hold);
  }
}
