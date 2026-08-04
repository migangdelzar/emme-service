package com.emme.identity.application.service;

import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.identity.api.result.FeatureFlagDetails;
import com.emme.identity.api.usecase.SetPlatformFeatureFlagUseCase;
import com.emme.identity.application.mapper.FeatureFlagApplicationMapper;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.domain.model.FeatureFlag;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the SetPlatformFeatureFlag use case. */
@Service
@Transactional
public class SetPlatformFeatureFlagService implements SetPlatformFeatureFlagUseCase {

  private final FeatureFlagRepository repository;

  public SetPlatformFeatureFlagService(FeatureFlagRepository repository) {
    this.repository = repository;
  }

  @Override
  public FeatureFlagDetails set(SetPlatformFeatureFlagCommand command) {
    Optional<FeatureFlag> existing =
        repository.findGlobalDefaults().stream()
            .filter(flag -> flag.code().equals(command.code()))
            .findFirst();
    FeatureFlag flag;
    if (existing.isPresent()) {
      flag = existing.get();
      flag.changeEnabled(command.enabled());
    } else {
      flag = new FeatureFlag(null, command.code(), command.enabled(), command.planRequired(), null);
    }
    return FeatureFlagApplicationMapper.toResult(repository.save(flag));
  }
}
