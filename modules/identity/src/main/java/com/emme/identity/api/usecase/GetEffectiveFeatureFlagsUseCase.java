package com.emme.identity.api.usecase;

import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.api.result.EffectiveFeatureFlags;

/** Resolves the effective feature-flag values for a tenant. */
public interface GetEffectiveFeatureFlagsUseCase {

  EffectiveFeatureFlags get(GetEffectiveFeatureFlagsQuery query);
}
