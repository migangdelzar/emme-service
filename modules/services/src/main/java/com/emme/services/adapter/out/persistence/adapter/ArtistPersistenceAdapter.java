package com.emme.services.adapter.out.persistence.adapter;

import com.emme.services.adapter.out.persistence.entity.ArtistEntity;
import com.emme.services.adapter.out.persistence.mapper.ArtistPersistenceMapper;
import com.emme.services.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.domain.model.Artist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the Artist persistence port using Spring Data JPA. */
@Component
public class ArtistPersistenceAdapter implements ArtistRepository {

  private final SpringDataArtistRepository repository;
  private final ArtistPersistenceMapper mapper;

  public ArtistPersistenceAdapter(SpringDataArtistRepository repository) {
    this.repository = repository;
    this.mapper = new ArtistPersistenceMapper();
  }

  @Override
  public Artist save(Artist artist) {
    ArtistEntity entity =
        artist.getId() == null
            ? mapper.toNewEntity(artist)
            : repository.findByTenantIdAndId(artist.getTenantId(), artist.getId()).orElseThrow();
    mapper.updateEntity(artist, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Artist> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Artist> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }
}
