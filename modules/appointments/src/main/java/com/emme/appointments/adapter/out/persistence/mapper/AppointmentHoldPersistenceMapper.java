package com.emme.appointments.adapter.out.persistence.mapper;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.adapter.out.persistence.entity.AppointmentHoldEntity;
import java.util.UUID;

/** Maps durable appointment holds without exposing JPA types to application contracts. */
public final class AppointmentHoldPersistenceMapper {

  public AppointmentHold toDomain(AppointmentHoldEntity entity) {
    return new AppointmentHold(
        entity.getId(),
        entity.getAppointmentId(),
        entity.getExpiresAt(),
        entity.getIdempotencyKey());
  }

  public AppointmentHoldEntity toNewEntity(AppointmentHold hold, UUID tenantId) {
    return new AppointmentHoldEntity(
        tenantId, hold.holdId(), hold.appointmentId(), hold.expiresAt(), hold.idempotencyKey());
  }

  public void updateEntity(AppointmentHold hold, AppointmentHoldEntity entity) {
    entity.setExpiresAt(hold.expiresAt());
  }
}
