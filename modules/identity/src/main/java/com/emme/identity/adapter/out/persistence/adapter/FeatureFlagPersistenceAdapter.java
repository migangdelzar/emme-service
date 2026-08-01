package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.adapter.out.persistence.mapper.FeatureFlagPersistenceMapper;
import com.emme.identity.adapter.out.persistence.repository.SpringDataFeatureFlagRepository;
import com.emme.identity.application.port.out.FeatureFlagRepository;
import com.emme.identity.domain.model.FeatureFlag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements feature-flag persistence without exposing JPA to the application layer. */
@Component
public final class FeatureFlagPersistenceAdapter implements FeatureFlagRepository {

  private final SpringDataFeatureFlagRepository repository;
  private final FeatureFlagPersistenceMapper mapper;

  public FeatureFlagPersistenceAdapter(
      SpringDataFeatureFlagRepository repository, FeatureFlagPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<FeatureFlag> findTenantOverride(UUID tenantId, String code) {
    return repository.findByTenantIdAndCode(tenantId, code).map(mapper::toDomain);
  }

  @Override
  public List<FeatureFlag> findGlobalDefaults() {
    return repository.findByTenantIdIsNull().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<FeatureFlag> findByTenantOrGlobal(UUID tenantId) {
    return repository.findByTenantIdOrTenantIdIsNull(tenantId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public FeatureFlag save(FeatureFlag featureFlag) {
    FeatureFlagEntity entity =
        featureFlag.id() == null
            ? mapper.toEntity(featureFlag)
            : repository
                .findById(featureFlag.id())
                .map(existing -> update(existing, featureFlag))
                .orElseGet(() -> mapper.toEntity(featureFlag));
    return mapper.toDomain(repository.save(entity));
  }

  private FeatureFlagEntity update(FeatureFlagEntity entity, FeatureFlag featureFlag) {
    entity.setEnabled(featureFlag.isEnabled());
    return entity;
  }
}
