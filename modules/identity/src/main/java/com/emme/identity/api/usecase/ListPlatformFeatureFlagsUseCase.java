package com.emme.identity.api.usecase;

import com.emme.identity.api.result.FeatureFlagDetails;
import java.util.List;

/** Lists the global feature flags managed by platform administrators. */
public interface ListPlatformFeatureFlagsUseCase {

  List<FeatureFlagDetails> list();
}
