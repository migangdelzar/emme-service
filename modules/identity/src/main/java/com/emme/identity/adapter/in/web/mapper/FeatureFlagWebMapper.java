package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.FeatureFlagResponse;
import com.emme.identity.api.result.FeatureFlagDetails;

/** Maps application feature-flag data into the existing HTTP response contract. */
public final class FeatureFlagWebMapper {

  private FeatureFlagWebMapper() {}

  public static FeatureFlagResponse toResponse(FeatureFlagDetails featureFlag) {
    return new FeatureFlagResponse(
        featureFlag.id(),
        featureFlag.code(),
        featureFlag.enabled(),
        featureFlag.planRequired(),
        featureFlag.description());
  }
}
