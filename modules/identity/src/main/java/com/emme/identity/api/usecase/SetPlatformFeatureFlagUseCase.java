package com.emme.identity.api.usecase;

import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.identity.api.result.FeatureFlagDetails;

/** Creates or updates a global feature flag. */
public interface SetPlatformFeatureFlagUseCase {

  FeatureFlagDetails set(SetPlatformFeatureFlagCommand command);
}
