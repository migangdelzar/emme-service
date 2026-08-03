package com.emme.studio.application.service;

import com.emme.studio.api.result.ArtistDetails;
import com.emme.studio.api.usecase.ListTenantArtistsUseCase;
import com.emme.studio.application.mapper.ArtistApplicationMapper;
import com.emme.studio.application.port.out.ArtistRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for listing artists in a tenant. */
@Service
@Transactional(readOnly = true)
public class ListTenantArtistsService implements ListTenantArtistsUseCase {

  private final ArtistRepository artistRepository;

  public ListTenantArtistsService(ArtistRepository artistRepository) {
    this.artistRepository = artistRepository;
  }

  @Override
  public List<ArtistDetails> list(UUID tenantId) {
    return artistRepository.findByTenantId(tenantId).stream()
        .map(ArtistApplicationMapper::toDetails)
        .toList();
  }
}
