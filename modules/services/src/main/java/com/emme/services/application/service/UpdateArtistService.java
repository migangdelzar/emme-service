package com.emme.services.application.service;

import com.emme.services.api.exception.StudioResourceNotFoundException;
import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.usecase.UpdateArtistUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.domain.model.Artist;
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
  public ArtistDetails update(UUID id, String name) {
    Artist artist =
        artistRepository
            .findById(id)
            .orElseThrow(() -> new StudioResourceNotFoundException("Artist", id));
    artist.setName(name);
    return ArtistApplicationMapper.toDetails(artistRepository.save(artist));
  }
}
