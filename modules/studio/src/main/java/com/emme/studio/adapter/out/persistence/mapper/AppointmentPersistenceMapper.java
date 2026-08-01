package com.emme.studio.adapter.out.persistence.mapper;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.domain.model.Appointment;

/** Translates the Appointment aggregate to and from its JPA representation. */
public final class AppointmentPersistenceMapper {

  public Appointment toDomain(AppointmentEntity entity) {
    return Appointment.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getCustomer().getId(),
        entity.getService().getId(),
        entity.getArtist().getId(),
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

  public AppointmentEntity toNewEntity(
      Appointment domain, CustomerEntity customer, ServiceEntity service, ArtistEntity artist) {
    AppointmentEntity entity =
        new AppointmentEntity(
            domain.getTenantId(),
            customer,
            service,
            artist,
            domain.getStartsAt(),
            domain.getEndsAt());
    updateEntity(domain, entity);
    return entity;
  }
}
