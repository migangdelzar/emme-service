package com.emme.identity.application.service;

import com.emme.identity.api.result.FeatureFlagDetails;
import com.emme.identity.api.usecase.ListPlatformFeatureFlagsUseCase;
import com.emme.identity.application.mapper.FeatureFlagApplicationMapper;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ListPlatformFeatureFlags use case. */
@Service
@Transactional(readOnly = true)
public class ListPlatformFeatureFlagsService implements ListPlatformFeatureFlagsUseCase {

  private final FeatureFlagRepository repository;

  public ListPlatformFeatureFlagsService(FeatureFlagRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<FeatureFlagDetails> list() {
    return repository.findGlobalDefaults().stream()
        .map(FeatureFlagApplicationMapper::toResult)
        .toList();
  }
}
