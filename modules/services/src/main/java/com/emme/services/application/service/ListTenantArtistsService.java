package com.emme.services.application.service;

import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.usecase.ListTenantArtistsUseCase;
import com.emme.services.application.mapper.ArtistApplicationMapper;
import com.emme.services.application.port.out.ArtistRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for listing artists in a tenant. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListTenantArtistsService implements ListTenantArtistsUseCase {

  private final ArtistRepository artistRepository;

  @Override
  public List<ArtistDetails> list(UUID tenantId) {
    return artistRepository.findAll().stream().map(ArtistApplicationMapper::toDetails).toList();
  }
}
