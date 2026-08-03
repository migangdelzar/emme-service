package com.emme.studio.application.service;

import com.emme.studio.api.result.ArtistDetails;
import com.emme.studio.api.usecase.GetArtistUseCase;
import com.emme.studio.application.mapper.ArtistApplicationMapper;
import com.emme.studio.application.port.out.ArtistRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for artist retrieval. */
@Service
@Transactional(readOnly = true)
public class GetArtistService implements GetArtistUseCase {

  private final ArtistRepository artistRepository;

  public GetArtistService(ArtistRepository artistRepository) {
    this.artistRepository = artistRepository;
  }

  @Override
  public Optional<ArtistDetails> get(UUID id) {
    return artistRepository.findById(id).map(ArtistApplicationMapper::toDetails);
  }
}
