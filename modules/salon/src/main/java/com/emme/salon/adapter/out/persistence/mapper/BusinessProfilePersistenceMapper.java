package com.emme.salon.adapter.out.persistence.mapper;

import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.domain.model.BusinessProfile;

/** Translates business-profile domain state to and from JPA. */
public final class BusinessProfilePersistenceMapper {

  public BusinessProfile toDomain(BusinessProfileEntity entity) {
    return BusinessProfile.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getTimeZone(),
        entity.getLocale(),
        entity.getDisplayName());
  }

  public void updateEntity(BusinessProfile domain, BusinessProfileEntity entity) {
    entity.setTimeZone(domain.getTimeZone());
    entity.setLocale(domain.getLocale());
    entity.setDisplayName(domain.getDisplayName());
  }

  public BusinessProfileEntity toNewEntity(BusinessProfile domain) {
    BusinessProfileEntity entity =
        new BusinessProfileEntity(
            domain.getTenantId(),
            domain.getTimeZone(),
            domain.getLocale(),
            domain.getDisplayName());
    updateEntity(domain, entity);
    return entity;
  }
}
