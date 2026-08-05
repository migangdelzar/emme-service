package com.emme.services.adapter.out.persistence.adapter;

import com.emme.services.adapter.out.persistence.entity.ArtistCapabilityEntity;
import com.emme.services.adapter.out.persistence.mapper.ArtistCapabilityPersistenceMapper;
import com.emme.services.adapter.out.persistence.repository.SpringDataArtistCapabilityRepository;
import com.emme.services.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.services.application.port.out.ArtistCapabilityRepository;
import com.emme.services.domain.model.ArtistCapability;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements artist capability persistence while resolving managed relationships. */
@Component
public class ArtistCapabilityPersistenceAdapter implements ArtistCapabilityRepository {

  private final SpringDataArtistCapabilityRepository repository;
  private final SpringDataArtistRepository artistRepository;
  private final SpringDataServiceRepository serviceRepository;
  private final ArtistCapabilityPersistenceMapper mapper;

  public ArtistCapabilityPersistenceAdapter(
      SpringDataArtistCapabilityRepository repository,
      SpringDataArtistRepository artistRepository,
      SpringDataServiceRepository serviceRepository) {
    this.repository = repository;
    this.artistRepository = artistRepository;
    this.serviceRepository = serviceRepository;
    this.mapper = new ArtistCapabilityPersistenceMapper();
  }

  @Override
  public ArtistCapability save(ArtistCapability capability) {
    ArtistCapabilityEntity entity =
        capability.getId() == null
            ? mapper.toNewEntity(
                capability,
                artistRepository.findById(capability.getArtist().getId()).orElseThrow(),
                serviceRepository.findById(capability.getService().getId()).orElseThrow())
            : repository.findById(capability.getId()).orElseThrow();
    mapper.updateEntity(capability, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<ArtistCapability> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<ArtistCapability> findByArtistId(UUID artistId) {
    return repository.findByArtistId(artistId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<ArtistCapability> findByServiceIdAndActive(UUID serviceId) {
    return repository.findByServiceIdAndActiveTrue(serviceId).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
