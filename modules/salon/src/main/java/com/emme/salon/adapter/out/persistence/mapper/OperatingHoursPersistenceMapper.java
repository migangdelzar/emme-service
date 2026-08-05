package com.emme.salon.adapter.out.persistence.mapper;

import com.emme.salon.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.salon.domain.model.OperatingHours;

/** Translates operating-hours domain state to and from JPA. */
public final class OperatingHoursPersistenceMapper {

  public OperatingHours toDomain(OperatingHoursEntity entity) {
    return OperatingHours.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getDayOfWeek(),
        entity.getOpensAt(),
        entity.getClosesAt(),
        entity.isActive());
  }

  public void updateEntity(OperatingHours domain, OperatingHoursEntity entity) {
    entity.setOpensAt(domain.getOpensAt());
    entity.setClosesAt(domain.getClosesAt());
    entity.setActive(domain.isActive());
  }

  public OperatingHoursEntity toNewEntity(OperatingHours domain) {
    OperatingHoursEntity entity =
        new OperatingHoursEntity(
            domain.getTenantId(), domain.getDayOfWeek(), domain.getOpensAt(), domain.getClosesAt());
    updateEntity(domain, entity);
    return entity;
  }
}
