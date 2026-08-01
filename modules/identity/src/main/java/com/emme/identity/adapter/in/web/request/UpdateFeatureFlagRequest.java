package com.emme.identity.adapter.in.web.request;

import com.emme.studio.subscriptions.api.type.PlanType;

/** Input used to update a platform feature flag. */
public record UpdateFeatureFlagRequest(boolean enabled, PlanType planRequired) {}
