package com.emme.identity.application.mapper;

import com.emme.identity.api.result.FeatureFlagInfo;
import com.emme.identity.domain.model.FeatureFlag;

/** Maps feature-flag domain objects to public application results. */
public final class FeatureFlagApplicationMapper {

  private FeatureFlagApplicationMapper() {}

  public static FeatureFlagInfo toInfo(FeatureFlag featureFlag) {
    return new FeatureFlagInfo(
        featureFlag.id(),
        featureFlag.code(),
        featureFlag.isEnabled(),
        featureFlag.planRequired(),
        featureFlag.description());
  }
}
