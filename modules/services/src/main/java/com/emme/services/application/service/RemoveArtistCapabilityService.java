package com.emme.services.application.service;

import com.emme.services.api.exception.StudioResourceNotFoundException;
import com.emme.services.api.result.ArtistCapabilityDetails;
import com.emme.services.api.usecase.RemoveArtistCapabilityUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistCapabilityRepository;
import com.emme.services.domain.model.ArtistCapability;
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
  public ArtistCapabilityDetails remove(UUID capabilityId) {
    ArtistCapability capability =
        artistCapabilityRepository
            .findById(capabilityId)
            .orElseThrow(() -> new StudioResourceNotFoundException("Capability", capabilityId));
    capability.deactivate();
    return ArtistApplicationMapper.toDetails(artistCapabilityRepository.save(capability));
  }
}
