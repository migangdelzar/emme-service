package com.emme.identity.api.usecase;

import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.api.result.FeatureFlagInfo;

/** Creates or updates a tenant-specific feature-flag override. */
public interface SetTenantFeatureFlagOverrideUseCase {

  FeatureFlagInfo set(SetTenantFeatureFlagOverrideCommand command);
}
