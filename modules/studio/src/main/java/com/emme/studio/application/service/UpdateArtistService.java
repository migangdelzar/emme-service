package com.emme.studio.application.service;

import com.emme.studio.api.exception.StudioResourceNotFoundException;
import com.emme.studio.api.usecase.UpdateArtistUseCase;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.domain.model.Artist;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for artist updates. */
@Service
@Transactional
public class UpdateArtistService implements UpdateArtistUseCase {

  private final ArtistRepository artistRepository;

  public UpdateArtistService(ArtistRepository artistRepository) {
    this.artistRepository = artistRepository;
  }

  @Override
  public Artist update(UUID id, String name) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new StudioResourceNotFoundException("Artist", id));
    artist.setName(name);
    return artistRepository.save(artist);
  }
}
