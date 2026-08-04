package com.emme.identity.application.mapper;

import com.emme.identity.api.result.FeatureFlagDetails;
import com.emme.identity.domain.model.FeatureFlag;

/** Maps feature-flag domain objects to public application results. */
public final class FeatureFlagApplicationMapper {

  private FeatureFlagApplicationMapper() {}

  public static FeatureFlagDetails toResult(FeatureFlag featureFlag) {
    return new FeatureFlagDetails(
        featureFlag.id(),
        featureFlag.code(),
        featureFlag.isEnabled(),
        featureFlag.planRequired(),
        featureFlag.description());
  }
}
