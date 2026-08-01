package com.emme.studio.application.service;

import com.emme.studio.api.usecase.DeactivateArtistUseCase;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.domain.model.Artist;
import jakarta.persistence.EntityNotFoundException;
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
  public Artist deactivate(UUID id) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Artist not found: " + id));
    artist.deactivate();
    return artistRepository.save(artist);
  }
}
