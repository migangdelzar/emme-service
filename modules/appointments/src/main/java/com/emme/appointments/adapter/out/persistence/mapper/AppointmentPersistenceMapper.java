package com.emme.appointments.adapter.out.persistence.mapper;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.appointments.domain.model.Appointment;

/** Translates the Appointment aggregate to and from its JPA representation. */
public final class AppointmentPersistenceMapper {

  public Appointment toDomain(AppointmentEntity entity) {
    return Appointment.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getCustomerId(),
        entity.getServiceId(),
        entity.getArtistId(),
        entity.getStartsAt(),
        entity.getEndsAt(),
        entity.getStatus(),
        entity.getExternalCalendarStatus());
  }

  public void updateEntity(Appointment domain, AppointmentEntity entity) {
    entity.setStartsAt(domain.getStartsAt());
    entity.setEndsAt(domain.getEndsAt());
    entity.setStatus(domain.getStatus());
    entity.setExternalCalendarStatus(domain.getExternalCalendarStatus());
  }

  public AppointmentEntity toNewEntity(Appointment domain) {
    AppointmentEntity entity =
        new AppointmentEntity(
            domain.getTenantId(),
            domain.getCustomerId(),
            domain.getServiceId(),
            domain.getArtistId(),
            domain.getStartsAt(),
            domain.getEndsAt());
    updateEntity(domain, entity);
    return entity;
  }
}
