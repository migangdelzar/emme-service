package com.emme.studio.application.service;

import com.emme.studio.api.exception.StudioResourceNotFoundException;
import com.emme.studio.api.usecase.AddArtistCapabilityUseCase;
import com.emme.studio.application.port.out.ArtistCapabilityRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Artist;
import com.emme.studio.domain.model.ArtistCapability;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Application service for assigning artist capabilities. */
@org.springframework.stereotype.Service
@Transactional
public class AddArtistCapabilityService implements AddArtistCapabilityUseCase {

  private final ArtistRepository artistRepository;
  private final ArtistCapabilityRepository artistCapabilityRepository;
  private final ServiceRepository serviceRepository;

  public AddArtistCapabilityService(
      ArtistRepository artistRepository,
      ArtistCapabilityRepository artistCapabilityRepository,
      ServiceRepository serviceRepository) {
    this.artistRepository = artistRepository;
    this.artistCapabilityRepository = artistCapabilityRepository;
    this.serviceRepository = serviceRepository;
  }

  @Override
  public ArtistCapability add(UUID artistId, UUID serviceId, UUID tenantId) {
    Artist artist =
        artistRepository
            .findById(artistId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Artist", artistId));
    com.emme.studio.domain.model.Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Service", serviceId));
    return artistCapabilityRepository.save(new ArtistCapability(tenantId, artist, service));
  }
}
