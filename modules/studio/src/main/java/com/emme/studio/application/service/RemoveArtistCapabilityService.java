package com.emme.studio.application.service;

import com.emme.studio.api.exception.StudioResourceNotFoundException;
import com.emme.studio.api.usecase.RemoveArtistCapabilityUseCase;
import com.emme.studio.application.port.out.ArtistCapabilityRepository;
import com.emme.studio.domain.model.ArtistCapability;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for removing artist capabilities. */
@Service
@Transactional
public class RemoveArtistCapabilityService implements RemoveArtistCapabilityUseCase {

  private final ArtistCapabilityRepository artistCapabilityRepository;

  public RemoveArtistCapabilityService(ArtistCapabilityRepository artistCapabilityRepository) {
    this.artistCapabilityRepository = artistCapabilityRepository;
  }

  @Override
  public ArtistCapability remove(UUID capabilityId) {
    ArtistCapability capability =
        artistCapabilityRepository
            .findById(capabilityId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Capability", capabilityId));
    capability.deactivate();
    return artistCapabilityRepository.save(capability);
  }
}
