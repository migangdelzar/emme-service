package com.emme.identity.application.service;

import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.api.result.FeatureFlagDetails;
import com.emme.identity.api.usecase.SetTenantFeatureFlagOverrideUseCase;
import com.emme.identity.application.mapper.FeatureFlagApplicationMapper;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.domain.model.FeatureFlag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the SetTenantFeatureFlagOverride use case. */
@Service
@Transactional
public class SetTenantFeatureFlagOverrideService implements SetTenantFeatureFlagOverrideUseCase {

  private final FeatureFlagRepository repository;

  public SetTenantFeatureFlagOverrideService(FeatureFlagRepository repository) {
    this.repository = repository;
  }

  @Override
  public FeatureFlagDetails set(SetTenantFeatureFlagOverrideCommand command) {
    return FeatureFlagApplicationMapper.toResult(
        setOverride(command.tenantId(), command.code(), command.enabled()));
  }

  /** Applies a tenant override to the domain model. */
  private FeatureFlag setOverride(UUID tenantId, String code, boolean enabled) {
    Optional<FeatureFlag> existing = repository.findTenantOverride(tenantId, code);
    if (existing.isPresent()) {
      FeatureFlag flag = existing.get();
      flag.changeEnabled(enabled);
      return repository.save(flag);
    }
    return repository.save(new FeatureFlag(tenantId, code, enabled, null, "Tenant override"));
  }
}
