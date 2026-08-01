package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.FeatureFlagResponse;
import com.emme.identity.domain.model.FeatureFlag;

/** Maps application feature-flag data into the existing HTTP response contract. */
public final class FeatureFlagWebMapper {

  private FeatureFlagWebMapper() {}

  public static FeatureFlagResponse toResponse(FeatureFlag featureFlag) {
    return new FeatureFlagResponse(
        featureFlag.id(),
        featureFlag.code(),
        featureFlag.isEnabled(),
        featureFlag.planRequired(),
        featureFlag.description());
  }
}
