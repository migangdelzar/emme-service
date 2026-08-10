package com.emme.services.application.service;

import com.emme.services.api.exception.StudioResourceNotFoundException;
import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.usecase.DeactivateArtistUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.domain.model.Artist;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for artist deactivation. */
@Service
@Transactional
public class DeactivateArtistService implements DeactivateArtistUseCase {

  private final ArtistRepository artistRepository;

  public DeactivateArtistService(ArtistRepository artistRepository) {
    this.artistRepository = artistRepository;
  }

  @Override
  public ArtistDetails deactivate(UUID id) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new StudioResourceNotFoundException("Artist", id));
    artist.deactivate();
    return ArtistApplicationMapper.toDetails(artistRepository.save(artist));
  }
}
