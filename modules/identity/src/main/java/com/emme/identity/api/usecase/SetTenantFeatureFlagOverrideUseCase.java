package com.emme.identity.api.usecase;

import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.api.result.FeatureFlagDetails;

/** Creates or updates a tenant-specific feature-flag override. */
public interface SetTenantFeatureFlagOverrideUseCase {

  FeatureFlagDetails set(SetTenantFeatureFlagOverrideCommand command);
}
