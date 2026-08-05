package com.emme.services.application.service;

import com.emme.services.api.exception.StudioResourceNotFoundException;
import com.emme.services.api.result.ArtistCapabilityDetails;
import com.emme.services.api.usecase.AddArtistCapabilityUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistCapabilityRepository;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.application.port.out.ServiceRepository;
import com.emme.services.domain.model.Artist;
import com.emme.services.domain.model.ArtistCapability;
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
  public ArtistCapabilityDetails add(UUID artistId, UUID serviceId, UUID tenantId) {
    Artist artist =
        artistRepository
            .findById(artistId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Artist", artistId));
    com.emme.services.domain.model.Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Service", serviceId));
    return ArtistApplicationMapper.toDetails(
        artistCapabilityRepository.save(new ArtistCapability(tenantId, artist, service)));
  }
}
