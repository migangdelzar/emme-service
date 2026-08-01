package com.emme.studio.application.service;

import com.emme.studio.api.usecase.ListArtistCapabilitiesUseCase;
import com.emme.studio.application.port.out.ArtistCapabilityRepository;
import com.emme.studio.domain.model.ArtistCapability;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for listing artist capabilities. */
@Service
@Transactional(readOnly = true)
public class ListArtistCapabilitiesService implements ListArtistCapabilitiesUseCase {

  private final ArtistCapabilityRepository artistCapabilityRepository;

  public ListArtistCapabilitiesService(ArtistCapabilityRepository artistCapabilityRepository) {
    this.artistCapabilityRepository = artistCapabilityRepository;
  }

  @Override
  public List<ArtistCapability> list(UUID artistId) {
    return artistCapabilityRepository.findByArtistId(artistId);
  }
}
