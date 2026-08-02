package com.emme.identity.api.usecase;

import com.emme.identity.api.result.FeatureFlagInfo;
import java.util.List;

/** Lists the global feature flags managed by platform administrators. */
public interface ListPlatformFeatureFlagsUseCase {

  List<FeatureFlagInfo> list();
}
