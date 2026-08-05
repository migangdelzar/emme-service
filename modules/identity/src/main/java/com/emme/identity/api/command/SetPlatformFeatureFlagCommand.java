package com.emme.identity.api.command;

import com.emme.subscriptions.api.type.PlanType;

/** Public intent to create or update a global feature flag. */
public record SetPlatformFeatureFlagCommand(String code, boolean enabled, PlanType planRequired) {}
