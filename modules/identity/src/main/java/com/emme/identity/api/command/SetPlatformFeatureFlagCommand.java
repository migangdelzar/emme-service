package com.emme.identity.api.command;

import com.emme.studio.subscriptions.api.PlanType;

/** Public intent to create or update a global feature flag. */
public record SetPlatformFeatureFlagCommand(String code, boolean enabled, PlanType planRequired) {}
