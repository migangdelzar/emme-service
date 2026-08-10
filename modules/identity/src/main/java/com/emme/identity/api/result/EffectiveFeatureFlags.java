package com.emme.identity.api.result;

import java.util.Map;

/** Public effective feature-flag values resolved for one tenant. */
public record EffectiveFeatureFlags(Map<String, Boolean> values) {

  public EffectiveFeatureFlags {
    values = Map.copyOf(values);
  }
}
