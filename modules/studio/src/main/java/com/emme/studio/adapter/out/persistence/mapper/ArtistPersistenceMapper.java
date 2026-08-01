package com.emme.studio.adapter.out.persistence.mapper;

import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.domain.model.Artist;

/** Translates the Artist domain model to and from its JPA representation. */
public final class ArtistPersistenceMapper {

  public Artist toDomain(ArtistEntity entity) {
    return Artist.reconstitute(
        entity.getId(), entity.getTenantId(), entity.getName(), entity.getStatus());
  }

  public void updateEntity(Artist domain, ArtistEntity entity) {
    entity.setName(domain.getName());
    entity.setStatus(domain.getStatus());
  }

  public ArtistEntity toNewEntity(Artist domain) {
    ArtistEntity entity = new ArtistEntity(domain.getTenantId(), domain.getName());
    updateEntity(domain, entity);
    return entity;
  }
}
