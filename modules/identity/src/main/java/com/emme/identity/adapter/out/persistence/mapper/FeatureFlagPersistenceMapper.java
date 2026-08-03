package com.emme.identity.adapter.out.persistence.mapper;

import com.emme.identity.adapter.out.persistence.entity.FeatureFlagEntity;
import com.emme.identity.domain.model.FeatureFlag;
import org.springframework.stereotype.Component;

/** Translates feature flags between the domain and JPA representations. */
@Component
public final class FeatureFlagPersistenceMapper {

  public FeatureFlagEntity toEntity(FeatureFlag featureFlag) {
    if (featureFlag.id() == null) {
      return new FeatureFlagEntity(
          featureFlag.tenantId(),
          featureFlag.code(),
          featureFlag.isEnabled(),
          featureFlag.planRequired(),
          featureFlag.description());
    }
    return FeatureFlagEntity.restore(
        featureFlag.id(),
        featureFlag.tenantId(),
        featureFlag.code(),
        featureFlag.isEnabled(),
        featureFlag.planRequired(),
        featureFlag.description(),
        featureFlag.createdAt(),
        featureFlag.updatedAt());
  }

  public FeatureFlag toDomain(FeatureFlagEntity entity) {
    return FeatureFlag.rehydrate(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.isEnabled(),
        entity.getPlanRequired(),
        entity.getDescription(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
