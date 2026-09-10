package com.emme.identity.application.service;

import com.emme.identity.api.result.FeatureFlagDetails;
import com.emme.identity.api.usecase.ListPlatformFeatureFlagsUseCase;
import com.emme.identity.application.mapper.FeatureFlagApplicationMapper;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ListPlatformFeatureFlags use case. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListPlatformFeatureFlagsService implements ListPlatformFeatureFlagsUseCase {

  private final FeatureFlagRepository repository;

  @Override
  public List<FeatureFlagDetails> list() {
    return repository.findGlobalDefaults().stream()
        .map(FeatureFlagApplicationMapper::toResult)
        .toList();
  }
}
