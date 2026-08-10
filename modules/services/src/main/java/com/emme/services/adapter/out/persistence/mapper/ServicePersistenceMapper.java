package com.emme.services.adapter.out.persistence.mapper;

import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.domain.model.Service;

/** Translates the Service domain model to and from its JPA representation. */
public final class ServicePersistenceMapper {

  public Service toDomain(ServiceEntity entity) {
    return Service.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getName(),
        entity.getCategory(),
        entity.getDescription(),
        entity.getDurationMinutes(),
        entity.getBasePrice(),
        entity.getStatus());
  }

  public void updateEntity(Service domain, ServiceEntity entity) {
    entity.setName(domain.getName());
    entity.setCategory(domain.getCategory());
    entity.setDescription(domain.getDescription());
    entity.setDurationMinutes(domain.getDurationMinutes());
    entity.setBasePrice(domain.getBasePrice());
    entity.setStatus(domain.getStatus());
  }

  public ServiceEntity toNewEntity(Service domain) {
    ServiceEntity entity =
        new ServiceEntity(
            domain.getTenantId(),
            domain.getCode(),
            domain.getName(),
            domain.getCategory(),
            domain.getDescription(),
            domain.getDurationMinutes(),
            domain.getBasePrice());
    updateEntity(domain, entity);
    return entity;
  }
}
