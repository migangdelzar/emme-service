package com.emme.services.adapter.out.persistence.mapper;

import com.emme.services.adapter.out.persistence.entity.ArtistCapabilityEntity;
import com.emme.services.adapter.out.persistence.entity.ArtistEntity;
import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.domain.model.ArtistCapability;

/** Translates artist capabilities between domain and JPA representations. */
public final class ArtistCapabilityPersistenceMapper {

  private final ArtistPersistenceMapper artistMapper = new ArtistPersistenceMapper();
  private final ServicePersistenceMapper serviceMapper = new ServicePersistenceMapper();

  public ArtistCapability toDomain(ArtistCapabilityEntity entity) {
    return ArtistCapability.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        artistMapper.toDomain(entity.getArtist()),
        serviceMapper.toDomain(entity.getService()),
        entity.isActive());
  }

  public ArtistCapabilityEntity toNewEntity(
      ArtistCapability domain, ArtistEntity artist, ServiceEntity service) {
    ArtistCapabilityEntity entity =
        new ArtistCapabilityEntity(domain.getTenantId(), artist, service);
    entity.setActive(domain.isActive());
    return entity;
  }

  public void updateEntity(ArtistCapability domain, ArtistCapabilityEntity entity) {
    entity.setActive(domain.isActive());
  }
}
