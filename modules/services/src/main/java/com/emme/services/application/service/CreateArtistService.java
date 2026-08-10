package com.emme.services.application.service;

import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.usecase.CreateArtistUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistRepository;
import com.emme.services.domain.model.Artist;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for artist creation. */
@Service
@Transactional
public class CreateArtistService implements CreateArtistUseCase {

  private final ArtistRepository artistRepository;

  public CreateArtistService(ArtistRepository artistRepository) {
    this.artistRepository = artistRepository;
  }

  @Override
  public ArtistDetails create(UUID tenantId, String name) {
    return ArtistApplicationMapper.toDetails(artistRepository.save(new Artist(tenantId, name)));
  }
}
