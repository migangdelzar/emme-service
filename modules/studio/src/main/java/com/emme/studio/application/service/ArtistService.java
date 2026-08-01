package com.emme.studio.application.service;

import com.emme.studio.application.port.out.ArtistCapabilityRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Artist;
import com.emme.studio.domain.model.ArtistCapability;
import com.emme.studio.domain.model.Service;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class ArtistService {

  private final ArtistRepository artistRepository;
  private final ArtistCapabilityRepository artistCapabilityRepository;
  private final ServiceRepository serviceRepository;

  public ArtistService(
      ArtistRepository artistRepository,
      ArtistCapabilityRepository artistCapabilityRepository,
      ServiceRepository serviceRepository) {
    this.artistRepository = artistRepository;
    this.artistCapabilityRepository = artistCapabilityRepository;
    this.serviceRepository = serviceRepository;
  }

  @Transactional(readOnly = true)
  public Optional<Artist> findById(UUID id) {
    return artistRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Artist> findByTenantId(UUID tenantId) {
    return artistRepository.findByTenantId(tenantId);
  }

  public Artist create(UUID tenantId, String name) {
    Artist artist = new Artist(tenantId, name);
    return artistRepository.save(artist);
  }

  public Artist update(UUID id, String name) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + id));
    artist.setName(name);
    return artistRepository.save(artist);
  }

  public Artist deactivate(UUID id) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + id));
    artist.deactivate();
    return artistRepository.save(artist);
  }

  public ArtistCapability addCapability(UUID artistId, UUID serviceId, UUID tenantId) {
    Artist artist =
        artistRepository
            .findById(artistId)
            .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + artistId));
    Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(() -> new EntityNotFoundException("Service not found: " + serviceId));
    ArtistCapability capability = new ArtistCapability(tenantId, artist, service);
    return artistCapabilityRepository.save(capability);
  }

  public ArtistCapability removeCapability(UUID capabilityId) {
    ArtistCapability capability =
        artistCapabilityRepository
            .findById(capabilityId)
            .orElseThrow(
                () -> new EntityNotFoundException("Capability not found: " + capabilityId));
    capability.deactivate();
    return artistCapabilityRepository.save(capability);
  }

  @Transactional(readOnly = true)
  public List<ArtistCapability> getCapabilities(UUID artistId) {
    return artistCapabilityRepository.findByArtistId(artistId);
  }
}
