package com.emme.identity.application.service;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
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
  private final Optional<SemanticCacheDependencyPublisher> cacheDependencies;

  public SetTenantFeatureFlagOverrideService(FeatureFlagRepository repository) {
    this(repository, Optional.empty());
  }

  public SetTenantFeatureFlagOverrideService(
      FeatureFlagRepository repository,
      Optional<SemanticCacheDependencyPublisher> cacheDependencies) {
    this.repository = repository;
    this.cacheDependencies = cacheDependencies;
  }

  @Override
  public FeatureFlagDetails set(SetTenantFeatureFlagOverrideCommand command) {
    FeatureFlagDetails result =
        FeatureFlagApplicationMapper.toResult(
            setOverride(command.tenantId(), command.code(), command.enabled()));
    cacheDependencies.ifPresent(
        publisher ->
            publisher.publish(
                new SemanticCacheDependencyChanged(
                    UUID.randomUUID(),
                    command.tenantId(),
                    null,
                    SemanticCacheDependencyChanged.Dependency.TENANT_POLICY,
                    command.code() + ":" + command.enabled(),
                    java.time.Instant.now())));
    return result;
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
