package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.ArtistCapabilityEntity;
import com.emme.studio.adapter.out.persistence.entity.ArtistEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistCapabilityRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.domain.model.ArtistStatus;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class ArtistService {

  private final SpringDataArtistRepository artistRepository;
  private final SpringDataArtistCapabilityRepository artistCapabilityRepository;
  private final SpringDataServiceRepository serviceRepository;

  public ArtistService(
      SpringDataArtistRepository artistRepository,
      SpringDataArtistCapabilityRepository artistCapabilityRepository,
      SpringDataServiceRepository serviceRepository) {
    this.artistRepository = artistRepository;
    this.artistCapabilityRepository = artistCapabilityRepository;
    this.serviceRepository = serviceRepository;
  }

  @Transactional(readOnly = true)
  public Optional<ArtistEntity> findById(UUID id) {
    return artistRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<ArtistEntity> findByTenantId(UUID tenantId) {
    return artistRepository.findByTenantId(tenantId);
  }

  public ArtistEntity create(UUID tenantId, String name) {
    ArtistEntity artist = new ArtistEntity(tenantId, name);
    return artistRepository.save(artist);
  }

  public ArtistEntity update(UUID id, String name) {
    ArtistEntity artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ArtistEntity not found: " + id));
    artist.setName(name);
    return artistRepository.save(artist);
  }

  public ArtistEntity deactivate(UUID id) {
    ArtistEntity artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ArtistEntity not found: " + id));
    artist.setStatus(ArtistStatus.INACTIVE);
    return artistRepository.save(artist);
  }

  public ArtistCapabilityEntity addCapability(UUID artistId, UUID serviceId, UUID tenantId) {
    ArtistEntity artist =
        artistRepository
            .findById(artistId)
            .orElseThrow(() -> new EntityNotFoundException("ArtistEntity not found: " + artistId));
    ServiceEntity service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(
                () -> new EntityNotFoundException("ServiceEntity not found: " + serviceId));
    ArtistCapabilityEntity capability = new ArtistCapabilityEntity(tenantId, artist, service);
    return artistCapabilityRepository.save(capability);
  }

  public ArtistCapabilityEntity removeCapability(UUID capabilityId) {
    ArtistCapabilityEntity capability =
        artistCapabilityRepository
            .findById(capabilityId)
            .orElseThrow(
                () -> new EntityNotFoundException("Capability not found: " + capabilityId));
    capability.setActive(false);
    return artistCapabilityRepository.save(capability);
  }

  @Transactional(readOnly = true)
  public List<ArtistCapabilityEntity> getCapabilities(UUID artistId) {
    return artistCapabilityRepository.findByArtistId(artistId);
  }
}
