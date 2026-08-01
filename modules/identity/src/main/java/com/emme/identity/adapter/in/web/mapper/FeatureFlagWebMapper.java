package com.emme.identity.adapter.in.web.mapper;

import com.emme.identity.adapter.in.web.response.FeatureFlagResponse;
import com.emme.identity.adapter.out.persistence.entity.FeatureFlag;

/** Maps persisted feature-flag data into the existing HTTP response contract. */
public final class FeatureFlagWebMapper {

  private FeatureFlagWebMapper() {}

  public static FeatureFlagResponse toResponse(FeatureFlag featureFlag) {
    return new FeatureFlagResponse(
        featureFlag.getId(),
        featureFlag.getCode(),
        featureFlag.isEnabled(),
        featureFlag.getPlanRequired(),
        featureFlag.getDescription());
  }
}
